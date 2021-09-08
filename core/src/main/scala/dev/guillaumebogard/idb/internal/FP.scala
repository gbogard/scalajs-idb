package dev.guillaumebogard.idb.internal

trait Monad[F[_]]:
  def pure[A](value: A): F[A]
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

trait Async[F[_]]:
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

object Async:
  def apply[F[_]](using async: Async[F]): Async[F] = async

extension [F[_], A](fa: F[A])(using monad: Monad[F])
  def map[B](f: A => B) = monad.map(fa)(f)
  def flatMap[B](f: A => F[B]) = monad.flatMap(fa)(f)
