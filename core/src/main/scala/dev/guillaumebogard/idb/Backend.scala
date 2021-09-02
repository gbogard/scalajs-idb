package dev.guillaumebogard.idb

import dev.guillaumebogard.idb.internal._

trait Backend[F[_]] {
  def open(database: String, version: Int): F[IDBDatabase]
}

  
