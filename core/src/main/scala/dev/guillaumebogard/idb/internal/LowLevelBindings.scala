package dev.guillaumebogard.idb.internal

import scala.scalajs.js
import dev.guillaumebogard.idb._
import scala.scalajs.js.annotation._
import js.JSConverters._

@js.native
@JSGlobal("indexedDB")
val indexedDB: IDBFactory = js.native

@js.native
trait IDBFactory extends js.Object {
  def open(name: IDBDatabase.Name): IDBOpenDBRequest
  def open(name: IDBDatabase.Name, version: Int): IDBOpenDBRequest
}

@js.native
trait IDBDatabase extends js.Object {
  val name: String = js.native
  val version: Int = js.native

  def close(): Unit = js.native

  def createObjectStore(
      name: IDBObjectStore.Name
  ): IDBObjectStore.WithOutOfLineKey = js.native

  def createObjectStore(
      name: IDBObjectStore.Name,
      options: IDBObjectStore.CreateObjectStoreOptions
  ): IDBObjectStore.WithInlineKey = js.native

  def transaction(stores: js.Array[IDBObjectStore.Name]): IDBTransaction = js.native

  def transaction(
      stores: js.Array[IDBObjectStore.Name],
      mode: IDBTransaction.Mode.JS
  ): IDBTransaction = js.native

}

extension (db: IDBDatabase)
  def transaction(store: IDBObjectStore.Name*): IDBTransaction = transaction(store.toJSArray)
  def transaction(mode: IDBTransaction.Mode, store: IDBObjectStore.Name*): IDBTransaction =
    transaction(store.toJSArray, mode.toJS)

object IDBDatabase:
  opaque type Name = String
  object Name:
    def apply(name: String): Name = name

@js.native
trait IDBRequest[Target, Result] extends js.Object {
  def result: Result | Null = js.native
  def error: DOMException | Null = js.native

  var onsuccess: js.Function1[DOMEvent[Target], Unit] = js.native
  var onerror: js.Function1[DOMEvent[Unit], Unit] = js.native

  @js.native
  trait Completed extends IDBRequest[Target, Result] {
    override def result: Result = js.native
  }

  @js.native
  trait Errored extends IDBRequest[Target, Result] {
    override def error: DOMException = js.native
  }
}

@js.native
trait IDBOpenDBRequest extends IDBRequest[IDBOpenDBRequest, IDBDatabase] {
  var onupgradeneeded: js.Function1[UpgradeNeededEvent, Unit] = js.native
  var onblocked: js.Function1[DOMEvent[IDBOpenDBRequest#Completed], Unit] = js.native
}

@js.native
trait AddRequest extends IDBRequest[AddRequest, Key]

@js.native
sealed trait IDBObjectStore extends js.Object {
  def get(key: Key): IDBRequest[Unit, js.UndefOr[js.Any]] = js.native
}

object IDBObjectStore:
  @JSExportAll
  case class CreateObjectStoreOptions(
      keyPath: KeyPath.JS,
      autoIncrement: js.UndefOr[Boolean]
  )

  @js.native
  trait WithInlineKey extends IDBObjectStore {
    def add(value: js.Object): IDBRequest[Unit, Unit] = js.native
  }
  @js.native
  trait WithOutOfLineKey extends IDBObjectStore {
    def add[Value <: js.Any](value: Value, key: Key): AddRequest = js.native
  }

  opaque type Name = String
  object Name:
    def apply(name: String): Name = name

@js.native
trait IDBTransaction extends js.Object {
  val db: IDBDatabase = js.native
  val mode: IDBTransaction.Mode.JS = js.native
  def objectStore[Store <: IDBObjectStore](name: IDBObjectStore.Name): Store = js.native
}

object IDBTransaction:
  enum Mode:
    case ReadWrite, ReadOnly

  object Mode:
    opaque type JS = String
    extension (mode: Mode)
      def toJS: JS =
        mode match
          case ReadOnly  => "readonly"
          case ReadWrite => "readwrite"
    given Conversion[Mode, JS] = toJS(_)

@js.native
trait DOMException extends js.Error

@js.native
trait DOMEvent[Target] extends js.Object {
  val target: Target = js.native
}

@js.native
trait UpgradeNeededEvent extends DOMEvent[IDBOpenDBRequest#Completed] {
  val oldVersion: Int = js.native
  val newVersion: Int = js.native
}
