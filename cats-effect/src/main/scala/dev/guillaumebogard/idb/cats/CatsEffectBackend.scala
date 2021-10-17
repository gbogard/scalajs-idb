package dev.guillaumebogard.idb.cats

import dev.guillaumebogard.idb.internal.Backend
import scala.concurrent.Future
import _root_.cats.effect.Async
import _root_.cats.effect.unsafe.IORuntime
import cats.implicits.given
import scala.concurrent.ExecutionContext

given [F[_]](using async: Async[F], runtime: IORuntime): Backend[F] with
  val executionContext = scala.concurrent.ExecutionContext.global
  def delay[A](thunk: => A): F[A] = async.delay(thunk)
  def fromFuture[A](fa: F[Future[A]]): F[A] = fa.flatMap { future =>
    given ec: ExecutionContext = executionContext
    async.async_(cb => future.onComplete(res => cb(res.toEither)))
  }
