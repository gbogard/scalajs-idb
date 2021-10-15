package dev.guillaumebogard.idb.internal

import dev.guillaumebogard.idb.api.{Schema, SchemaOperation}

private[internal] object SchemaExecutor:
  def unsafeMigrateSchema(event: UpgradeNeededEvent, schema: Schema): Unit = {
    val db = event.target.result
    schema
      .getPendingOperations(event.oldVersion)
      .foreach({ case SchemaOperation.CreateObjectStore(name) =>
        db.createObjectStore(name)
      })
  }
