package dev.guillaumebogard.idb.api

import dev.guillaumebogard.idb.api

type Schema = Schema.Schema

object Schema:
  opaque type Schema = Vector[SchemaOperation]

  def apply(): Schema = Vector.empty

  extension (schema: Schema)
    def createObjectStore(name: api.ObjectStore.Name): Schema = schema :+ SchemaOperation.CreateObjectStore(name)
    def lastVersion: Int = schema.length + 1
    def getPendingOperations(oldVersion: Int): Vector[SchemaOperation] =
      schema.drop(Math.max(0, oldVersion - 1))


enum SchemaOperation:
  case CreateObjectStore(name: api.ObjectStore.Name)
