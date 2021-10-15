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

import cats.data.NonEmptyList
import cats.implicits.*
import cats.free.Free
import dev.guillaumebogard.idb.internal

trait Database[F[_]]:
  def transact[T](mode: Transaction.Mode, stores: NonEmptyList[ObjectStore.Name])(transaction: Transaction[T]): F[T]

  def readOnly[T](stores: NonEmptyList[ObjectStore.Name])(transaction: Transaction[T]): F[T] =
    transact(Transaction.Mode.ReadOnly, stores)(transaction)

  def readWrite[T](stores: NonEmptyList[ObjectStore.Name])(transaction: Transaction[T]): F[T] =
    transact(Transaction.Mode.ReadWrite, stores)(transaction)

  def getObjectStore(name: ObjectStore.Name): Transaction[ObjectStore] =
    Transaction.getObjectStore(name)

object Database:

  def open[F[_]: Backend](name: Name, schema: Schema): F[Either[Throwable, Database[F]]] =
    internal.Backend.openDatabase(name, schema)

  opaque type Name = String
  object Name:
    def apply(name: String): Name = name
