/*
 * Copyright 2021 Guillaume Bogard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

trait MonadError[F[_], E] extends Monad[F]:
  def raiseError[A](e: E): F[A]
  def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A]
  def attempt[A](fa: F[A]): F[Either[E, A]] =
    handleErrorWith(map[A, Either[E, A]](fa)(Right(_)))(e => pure(Left(e)))

object MonadError:
  def apply[F[_], E](using me: MonadError[F, E]): MonadError[F, E] = me

  extension [F[_], A, E](fa: F[A])(using me: MonadError[F, E]) def attempt: F[Either[E, A]] = me.attempt(fa)

  given (using ExecutionContext): MonadError[Future, Throwable] with
    def pure[A](value: A): Future[A] = Monad[Future].pure(value)
    def map[A, B](fa: Future[A])(f: A => B): Future[B] = Monad[Future].map(fa)(f)
    def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = Monad[Future].flatMap(fa)(f)
    def raiseError[A](e: Throwable): Future[A] = Future.failed(e)
    def handleErrorWith[A](fa: Future[A])(f: Throwable => Future[A]): Future[A] = fa.recoverWith({ case e =>
      f(e)
    })

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
