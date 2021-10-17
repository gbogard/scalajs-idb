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
