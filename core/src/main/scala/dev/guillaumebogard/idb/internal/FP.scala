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

import scala.concurrent.*

/** The [[Async]] type class, ported from Cats Effect to avoid unnecessary dependencies in the core
  * project.
  */
trait Async[F[_]]:
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A]

object Async:
  def apply[F[_]](using async: Async[F]): Async[F] = async

  given Async[Future] with
    def async[A](k: (Either[Throwable, A] => Unit) => Unit): Future[A] =
      val promise = Promise[A]()
      val cb: Either[Throwable, A] => Unit =
        case Right(res) => promise.success(res)
        case Left(err)  => promise.failure(err)
      k(cb)
      promise.future
