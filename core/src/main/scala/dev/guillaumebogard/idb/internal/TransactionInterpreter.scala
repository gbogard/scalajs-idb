package dev.guillaumebogard.idb.internal

import cats.*
import cats.data.State
import dev.guillaumebogard.idb.api.*

object TransactionInterpreter:
  private type StoreCollectorM[A] = State[Vector[ObjectStore.Name], A]

  /** The store collector is responsible for turning transactions into sequences of [[ObjectStore.Name]] that
    * can then be fed to IndexedDB to open a transaction
    */
  private object StoreCollector extends (TransactionA ~> StoreCollectorM):
    def apply[A](fa: TransactionA[A]): StoreCollectorM[A] = fa match {
      case TransactionA.GetObjectStore(name) =>
        State.modify[Vector[ObjectStore.Name]](_ :+ name).map(_.asInstanceOf[A])
      case _ => Monad[StoreCollectorM].unit.map(_.asInstanceOf[A])
    }

  private def collectStores[T](tx: Transaction[T]): Vector[ObjectStore.Name] =
    tx.foldMap(StoreCollector).run(Vector.empty).map(_._1).value
