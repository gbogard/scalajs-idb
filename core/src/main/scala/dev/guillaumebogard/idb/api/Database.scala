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

package dev.guillaumebogard.idb.api

import cats.implicits.*
import dev.guillaumebogard.idb.Backend
import dev.guillaumebogard.idb.Backend.given
import dev.guillaumebogard.idb.api.schema.*
import dev.guillaumebogard.idb.internal

trait Database[F[_]]:
  def transact[T](transaction: Transaction[T]): F[T]

object Database:

  def open[F[_]: Backend](name: Name, schema: Schema): F[Either[Throwable, Database[F]]] =
    // TODO: handle upgrade event
    internal
      .runOpenRequest(_ => ())(Backend[F].buildOpenRequest(name, schema.lastVersion))
      .map(res => fromJS(res.database))
      .attempt

  private def fromJS[F[_]: Backend](db: internal.IDBDatabase) =
    new Database[F]:
      def transact[T](transaction: Transaction[T]) = ???

  opaque type Name = String
  object Name:
    def apply(name: String): Name = name