package dev.guillaumebogard.idb.api

import dev.guillaumebogard.idb.Backend
import dev.guillaumebogard.idb.internal

trait IDBDatabase[F[_]]:
  def transaction(stores: IDBObjectStore.Name*): F[internal.IDBTransaction] = ???

  def transaction(
      stores: List[IDBObjectStore.Name],
      mode: IDBTransaction.Mode.JS
  ): IDBTransaction[F] = ???

object IDBDatabase:

  def open[F[_]: Backend](name: Name): F[Either[Throwable, IDBDatabase[F]]] = ???

  opaque type Name = String
  object Name:
    def apply(name: String): Name = name

