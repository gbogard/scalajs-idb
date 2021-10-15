package dev.guillaumebogard.idb.internal

import cats.*
import cats.implicits.*
import cats.data.NonEmptyList
import dev.guillaumebogard.idb.api
import dev.guillaumebogard.idb.internal.Backend.given

private[internal] object TransactionInterpreter:
  def apply[F[_], A](
      db: IDBDatabase,
      stores: NonEmptyList[api.ObjectStore.Name],
      mode: api.Transaction.Mode,
      tx: api.Transaction[A]
  )(using
      backend: Backend[F]
  ): F[A] = {
    import backend.given
    backend
      .buildTransaction(db, stores, mode)
      .flatMap(idbTransaction => tx.foldMap(new Transactor(idbTransaction)))
  }

  private def buildStore(_name: api.ObjectStore.Name) = (new api.ObjectStore { val name = _name })

  private class Transactor[F[_]](idbTransaction: IDBTransaction)(using backend: Backend[F])
      extends (api.TransactionA ~> F):
    import backend.given

    def apply[A](xa: api.TransactionA[A]): F[A] = xa match {
      case api.TransactionA.GetObjectStore(name) => buildStore(name).pure[F]
      case api.TransactionA.Get(store, key) =>
        backend
          .runRequest(backend.delay(idbTransaction.objectStore(store).get(key)))
          .map(_.result.toOption.asInstanceOf[A])
      case api.TransactionA.Put(store, value, key) =>
        backend
          .runRequest(backend.delay(idbTransaction.objectStore(store).put(value, key)))
          .void
          .map(_.asInstanceOf[A])
    }
