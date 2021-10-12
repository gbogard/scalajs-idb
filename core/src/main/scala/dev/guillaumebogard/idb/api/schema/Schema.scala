package dev.guillaumebogard.idb.api.schema

import dev.guillaumebogard.idb.api

type Schema = Schema.Schema

object Schema:
  opaque type Schema = Vector[SchemaOperation]

  def apply(): Schema = Vector.empty

  extension (schema: Schema)
    def lastVersion: Int = schema.length - 1
    def createObjectStore(name: api.IDBObjectStore.Name) = schema :+ SchemaOperation.CreateObjectStore(name)

enum SchemaOperation:
  case CreateObjectStore(name: api.IDBObjectStore.Name)
