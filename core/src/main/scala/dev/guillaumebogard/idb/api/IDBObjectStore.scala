package dev.guillaumebogard.idb.api

trait IDBObjectStore[F[_]]

object IDBObjectStore:
  opaque type Name = String
  object Name:
    def apply(name: String): Name = name
