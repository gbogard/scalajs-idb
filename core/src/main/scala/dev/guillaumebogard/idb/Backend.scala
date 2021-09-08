package dev.guillaumebogard.idb

import dev.guillaumebogard.idb.internal._
import scala.concurrent._
import scala.scalajs.js
import Backend._

trait Backend[F[_]] extends Monad[F] with Async[F]

given (using ec: ExecutionContext): Backend[Future] with
  def pure[A](value: A): Future[A] = Future.successful(value)
  def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): Future[A] = {
    val promise = Promise[A]()
    val cb: Either[Throwable, A] => Unit =
      case Right(res) => promise.success(res)
      case Left(err)  => promise.failure(err)
    k(cb)
    promise.future
  }

object Backend:
  def runRequest[F[_]: Backend, T, R](reqF: F[IDBRequest[T, R]]): F[IDBRequestResult[T, R]] =
    reqF.flatMap { req =>
      Async[F].async { cb =>
        req.onsuccess = event => {
          val result = IDBRequestResult(req, event, req.result.asInstanceOf[R])
          cb(Right(result))
        }
        req.onerror = _ => {
          cb(Left(js.JavaScriptException(req.error.asInstanceOf[DOMException])))
        }
      }
    }

  def runOpenRequest[F[_]: Backend, R](upgrade: UpgradeNeededEvent => R)(
      reqF: F[IDBOpenDBRequest]
  ): F[IDBOpenResult[R]] =
    reqF.flatMap { req =>
      var upgradeResult: Option[R] = None
      Async[F].async { cb =>
        req.onsuccess = event =>
          cb(Right(IDBOpenResult(req, event, event.target.result.asInstanceOf[IDBDatabase], upgradeResult)))
        req.onerror = _ => cb(Left(js.JavaScriptException(req.error.asInstanceOf[DOMException])))
        req.onupgradeneeded = event => {
          upgradeResult = Some(upgrade(event))
        }
      }
    }

  final case class IDBRequestResult[Target, Result](
      req: IDBRequest[Target, Result],
      event: DOMEvent[Target],
      result: Result
  )

  final case class IDBOpenResult[UpgradeResult](
      req: IDBOpenDBRequest,
      event: DOMEvent[IDBOpenDBRequest],
      database: IDBDatabase,
      upgradeResult: Option[UpgradeResult]
  )
