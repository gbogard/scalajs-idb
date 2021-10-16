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

private def buildStore(_name: api.ObjectStore.Name) = (new api.ObjectStore { val name = _name })

private class Transactor(idbTransaction: IDBTransaction)(using ec: ExecutionContext)
    extends (api.TransactionA ~> Future):
  def apply[A](xa: api.TransactionA[A]): Future[A] = xa match {
    case api.TransactionA.GetObjectStore(name) => buildStore(name).pure[Future]
    case api.TransactionA.Get(store, key) =>
      idbTransaction.objectStoreFuture(store).flatMap(_.getFuture(key))
    case api.TransactionA.Put(store, value, key) =>
      idbTransaction.objectStoreFuture(store).flatMap(_.putFuture(value, key))
    case api.TransactionA.Add(store, value, key) =>
      idbTransaction.objectStoreFuture(store).flatMap(_.addFuture(value, key))
  }
