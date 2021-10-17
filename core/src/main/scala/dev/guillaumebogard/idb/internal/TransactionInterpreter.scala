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

import cats.*
import cats.implicits.*
import cats.data.NonEmptyList
import dev.guillaumebogard.idb.api
import dev.guillaumebogard.idb.internal.future.*
import dev.guillaumebogard.idb.internal.lowlevel.*
import scala.concurrent.{Future, ExecutionContext}

extension (db: IDBDatabase)
  private[internal] def transactFuture[A](
      stores: NonEmptyList[api.ObjectStore.Name],
      mode: api.Transaction.Mode
  )(
      tx: api.Transaction[A]
  )(using ec: ExecutionContext): Future[A] =
    db.transactionFuture(stores.toList, mode)
      .flatMap(idbTransaction => tx.foldMap(new Transactor(idbTransaction)))

private class Transactor(idbTransaction: IDBTransaction)(using ec: ExecutionContext)
    extends (api.TransactionA ~> Future):
  def apply[A](xa: api.TransactionA[A]): Future[A] = xa match {
    case api.TransactionA.Get(store, key) =>
      idbTransaction.objectStoreFuture(store).flatMap(_.getFuture(key))
    case api.TransactionA.Put(store, value, key) =>
      idbTransaction.objectStoreFuture(store).flatMap(_.putFuture(value, key))
    case api.TransactionA.Add(store, value, key) =>
      idbTransaction.objectStoreFuture(store).flatMap(_.addFuture(value, key))
  }
