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

import dev.guillaumebogard.idb.Backend
import dev.guillaumebogard.idb.given
import dev.guillaumebogard.idb.internal
import dev.guillaumebogard.idb.internal.MonadError.*
import dev.guillaumebogard.idb.internal.Monad.*
import dev.guillaumebogard.idb.Backend.given
import dev.guillaumebogard.idb.api.schema.*

trait IDBDatabase[F[_]]:
  def transaction(stores: IDBObjectStore.Name*): F[IDBTransaction[F]]
  def transaction(stores: List[IDBObjectStore.Name], mode: IDBTransaction.Mode): F[IDBTransaction[F]]

object IDBDatabase:

  def open[F[_]: Backend](name: Name, schema: Schema): F[Either[Throwable, IDBDatabase[F]]] =
    // TODO: handle upgrade event
    Backend
      .runOpenRequest(_ => ())(Backend[F].buildOpenRequest(name, schema.lastVersion))
      .map(res => fromJS(res.database))
      .attempt

  private def fromJS[F[_]: Backend](db: internal.IDBDatabase): IDBDatabase[F] =
    new IDBDatabase[F]:
      def transaction(stores: IDBObjectStore.Name*): F[IDBTransaction[F]] = ???
      def transaction(stores: List[IDBObjectStore.Name], mode: IDBTransaction.Mode): F[IDBTransaction[F]] =
        ???

  opaque type Name = String
  object Name:
    def apply(name: String): Name = name
