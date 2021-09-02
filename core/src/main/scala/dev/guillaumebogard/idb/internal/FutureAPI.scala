package dev.guillaumebogard.idb.internal

import scala.concurrent._
import scala.scalajs.js

final case class OpenFutureResult(
    open: Future[IDBDatabase],
    upgrade: Future[Option[UpgradeNeededEvent]]
)

extension (factory: IDBFactory)
  def openFuture(name: String, version: Int): OpenFutureResult =
    toResult(factory.open(name, version))
  def openFuture(name: String): OpenFutureResult =
    toResult(factory.open(name))

  private def toResult(db: IDBOpenDBRequest) = {
    val openP = Promise[IDBDatabase]()
    val upgradeP = Promise[Option[UpgradeNeededEvent]]
    db.onsuccess = event => {
      if (!upgradeP.isCompleted) upgradeP.success(None)
      openP.success(event.target.result.asInstanceOf[IDBDatabase])
    }
    db.onerror = _ =>
      openP.failure(
        js.JavaScriptException(db.error.asInstanceOf[DOMException])
      )
    db.onupgradeneeded = event => upgradeP.success(Some(event))
    OpenFutureResult(openP.future, upgradeP.future)
  }
