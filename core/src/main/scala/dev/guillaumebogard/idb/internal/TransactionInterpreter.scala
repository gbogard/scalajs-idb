package dev.guillaumebogard.idb.internal

import cats.*
import cats.implicits.*
import cats.data.State
import dev.guillaumebogard.idb.api
import dev.guillaumebogard.idb.internal.Backend.given

private[internal] object TransactionInterpreter:
  def apply[F[_], A](db: IDBDatabase, mode: api.Transaction.Mode, tx: api.Transaction[A])(using
      backend: Backend[F]
  ): F[A] = {
    import backend.given
    backend
      .buildTransaction(db, collectStores(tx), mode)
      .flatMap(idbTransaction => tx.foldMap(new Transactor(idbTransaction)))
  }

  private type StoreCollectorM[A] = State[Vector[api.ObjectStore.Name], A]

  /** The store collector is responsible for turning transactions into sequences of [[ObjectStore.Name]] that
    * can then be fed to IndexedDB to open a transaction
    */
  private object StoreCollector extends (api.TransactionA ~> StoreCollectorM):
    def apply[A](xa: api.TransactionA[A]): StoreCollectorM[A] = xa match {
      case api.TransactionA.GetObjectStore(name) =>
        State.modify[Vector[api.ObjectStore.Name]](_ :+ name).map(_.asInstanceOf[A])
      case _ => Monad[StoreCollectorM].unit.map(_.asInstanceOf[A])
    }

  private def collectStores[T](tx: api.Transaction[T]): Vector[api.ObjectStore.Name] =
    tx.foldMap(StoreCollector).run(Vector.empty).map(_._1).value

  private class Transactor[F[_]](idbTransaction: IDBTransaction)(using backend: Backend[F])
      extends (api.TransactionA ~> F):
    import backend.given

    def apply[A](xa: api.TransactionA[A]): F[A] = xa match {
      case api.TransactionA.GetObjectStore(_name) =>
        (new api.ObjectStore { val name = _name }).pure[F]
      case api.TransactionA.Get(store, key) =>
        backend
          .runRequest(backend.delay(idbTransaction.objectStore(store).get(key)))
          .map(_.result.get.asInstanceOf[A])
      case api.TransactionA.Put(store, value, key) =>
        backend
          .runRequest(backend.delay(idbTransaction.objectStore(store).add(value, key)))
          .map(_.asInstanceOf[A])
    }
