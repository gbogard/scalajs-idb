package dev.guillaumebogard.idb.internal

import scala.concurrent._

trait Monad[F[_]]:
  def pure[A](value: A): F[A]
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

object Monad:
  def apply[F[_]](using Monad[F]): Monad[F] = summon[Monad[F]]

extension [F[_], A](fa: F[A])(using monad: Monad[F])
  def map[B](f: A => B) = monad.map(fa)(f)
  def flatMap[B](f: A => F[B]) = monad.flatMap(fa)(f)

given (using ExecutionContext): Monad[Future] with
  def pure[A](value: A): Future[A] = Future.successful(value)
  def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

trait Async[F[_]]:
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

object Async:
  def apply[F[_]](using async: Async[F]): Async[F] = async

given Async[Future] with
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): Future[A] = {
    val promise = Promise[A]()
    val cb: Either[Throwable, A] => Unit =
      case Right(res) => promise.success(res)
      case Left(err)  => promise.failure(err)
    k(cb)
    promise.future
  }
