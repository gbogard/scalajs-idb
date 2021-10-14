package dev.guillaumebogard.idb.internal

import dev.guillaumebogard.idb.api.Schema

private[internal] object SchemaExecutor:
  def unsafeMigrateSchema(event: UpgradeNeededEvent, schema: Schema): Unit = ???
