package dev.guillaumebogard.idb.api

import dev.guillaumebogard.idb.api

type Schema = Schema.Schema

object Schema:
  opaque type Schema = Vector[SchemaOperation]

  def apply(): Schema = Vector.empty

  extension (schema: Schema)
    def lastVersion: Int = schema.length - 1
    def createObjectStore(name: api.ObjectStore.Name) = schema :+ SchemaOperation.CreateObjectStore(name)

enum SchemaOperation:
  case CreateObjectStore(name: api.ObjectStore.Name)
