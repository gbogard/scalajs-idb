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
import dev.guillaumebogard.idb.internal.IDBDatabase
import cats.free.Free

trait Database[F[_]]:
  def transact[T](mode: Transaction.Mode)(transaction: Transaction[T]): F[T]
  def getObjectStore(name: ObjectStore.Name): Transaction[ObjectStore] =
    Transaction.getObjectStore(name)

object Database:

  def open[F[_]](name: Name, schema: Schema)(using backend: Backend[F]): F[Either[Throwable, Database[F]]] = {
    import backend.given

    // TODO: handle upgrade event
    backend
      .runOpenRequest(_ => ())(backend.buildOpenRequest(name, schema.lastVersion))
      .map(res => fromJS(res.database))
      .attempt
  }

  private def fromJS[F[_]: Backend](db: IDBDatabase) =
    new Database[F]:
      def transact[T](mode: Transaction.Mode)(transaction: Transaction[T]) = ???

  opaque type Name = String
  object Name:
    def apply(name: String): Name = name
