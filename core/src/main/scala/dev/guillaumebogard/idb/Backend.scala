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

package dev.guillaumebogard.idb

import dev.guillaumebogard.idb.internal.*
import cats.MonadError
import cats.implicits.given
import scala.concurrent.*
import scala.scalajs.js
import Backend.*
import scala.util.Try
import java.util.Arrays

trait Backend[F[_]]:
  val monadF: MonadError[F, Throwable]
  val asyncF: Async[F]

  /** Builds a [[IDBOpenDBRequest]] that an then be passed to [[Backend.runOpenRequest]] to open a new
    * database
    */
  def buildOpenRequest(name: api.Database.Name, version: Int): F[IDBOpenDBRequest]

object Backend:
  def apply[F[_]](using b: Backend[F]): Backend[F] = b

  given [F[_]](using backend: Backend[F]): MonadError[F, Throwable] = backend.monadF
  given [F[_]](using backend: Backend[F]): Async[F] = backend.asyncF

  given (using ec: ExecutionContext): Backend[Future] with
    val monadF = MonadError[Future, Throwable]
    val asyncF = Async[Future]
    def buildOpenRequest(name: api.Database.Name, version: Int) =
      Future.fromTry(Try(indexedDB.open(name, version)))



