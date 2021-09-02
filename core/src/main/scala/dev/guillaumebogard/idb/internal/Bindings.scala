package dev.guillaumebogard.idb.internal

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSGlobal("indexedDB")
val indexedDb: IDBFactory = js.native

@js.native
trait IDBFactory extends js.Object {
  def open(name: String): IDBOpenDBRequest
  def open(name: String, version: Int): IDBOpenDBRequest
}

@js.native
trait IDBDatabase extends js.Object {
  val name: String
  val version: Int

  def close(): Unit = js.native
  def createObjectStore(name: String): IDBObjectStore
  def createObjectStore(
      name: String,
      options: CreateObjectStoreOptions
  ): IDBObjectStore
}

@js.native
trait IDBRequest[Target, Result] extends js.Object {
  def result: Result | Null = js.native
  def error: DOMException | Null = js.native

  var onsuccess: js.Function1[DOMEvent[Target], Unit] = js.native
  var onerror: js.Function1[DOMEvent[Unit], Unit] = js.native
}

@js.native
trait IDBOpenDBRequest extends IDBRequest[IDBOpenDBRequest, IDBDatabase] {
  var onupgradeneeded: js.Function1[UpgradeNeededEvent, Unit] = js.native
  var onblocked: js.Function1[DOMEvent[IDBOpenDBRequest], Unit] = js.native
}

@JSExportAll
case class CreateObjectStoreOptions(
    keyPath: KeyPath.JS,
    autoIncrement: js.UndefOr[Boolean]
)

@js.native
trait IDBObjectStore extends js.Object

@js.native
trait DOMException extends js.Error

@js.native
trait DOMEvent[Target] extends js.Object {
  val target: Target = js.native
}

@js.native
trait UpgradeNeededEvent extends DOMEvent[IDBDatabase] {
  val oldVersion: Int = js.native
  val newVersion: Int = js.native
}
