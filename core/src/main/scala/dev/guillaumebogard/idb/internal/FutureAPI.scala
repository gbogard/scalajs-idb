package dev.guillaumebogard.idb.internal

import scala.concurrent._
import scala.scalajs.js

object FutureAPI:

  final case class OpenFutureResult[UpgradeResult](
      open: Future[IDBDatabase],
      upgrade: Future[Option[UpgradeResult]]
  )

  extension (factory: IDBFactory)
    def openFuture[UpgradeResult](
        name: String,
        version: Int,
        onUpgradeNeeded: UpgradeNeededEvent => UpgradeResult
    ): OpenFutureResult[UpgradeResult] =
      toResult(factory.open(name, version), onUpgradeNeeded)

    def openFuture[UpgradeResult](
        name: String,
        onUpgradeNeeded: UpgradeNeededEvent => UpgradeResult =
          identity[UpgradeNeededEvent]
    ): OpenFutureResult[UpgradeResult] =
      toResult(factory.open(name), onUpgradeNeeded)

  private def toResult[UpgradeResult](
      db: IDBOpenDBRequest,
      onUpgradeNeeded: UpgradeNeededEvent => UpgradeResult =
        identity[UpgradeNeededEvent]
  ) = {
    val openP = Promise[IDBDatabase]()
    val upgradeP = Promise[Option[UpgradeResult]]
    db.onsuccess = event => {
      if (!upgradeP.isCompleted) upgradeP.success(None)
      openP.success(event.target.result.asInstanceOf[IDBDatabase])
    }
    db.onerror = _ =>
      openP.failure(
        js.JavaScriptException(db.error.asInstanceOf[DOMException])
      )
    db.onupgradeneeded = event => upgradeP.success(Some(onUpgradeNeeded(event)))
    OpenFutureResult[UpgradeResult](openP.future, upgradeP.future)
  }

end FutureAPI
