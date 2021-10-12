/*
 * Copyright 2021 Guillaume Bogard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This module contains the low-level, impure, imperative interface to IndexedDB,
 * as described [here](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API).
 */

package dev.guillaumebogard.idb.internal

import scala.scalajs.js
import dev.guillaumebogard.idb._
import dev.guillaumebogard.idb.api._
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
      name: api.IDBObjectStore.Name
  ): IDBObjectStore.WithOutOfLineKey = js.native

  def createObjectStore(
      name: api.IDBObjectStore.Name,
      options: IDBObjectStore.CreateObjectStoreOptions
  ): IDBObjectStore.WithInlineKey = js.native

  def transaction(stores: js.Array[api.IDBObjectStore.Name]): IDBTransaction = js.native

  def transaction(
      stores: js.Array[api.IDBObjectStore.Name],
      mode: IDBTransaction.Mode.JS
  ): IDBTransaction = js.native

}

extension (db: IDBDatabase)
  def transaction(store: api.IDBObjectStore.Name*): IDBTransaction = transaction(store.toJSArray)
  def transaction(mode: IDBTransaction.Mode, store: api.IDBObjectStore.Name*): IDBTransaction =
    transaction(store.toJSArray, mode.toJS)

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

@js.native
trait IDBTransaction extends js.Object {
  val db: IDBDatabase = js.native
  val mode: IDBTransaction.Mode.JS = js.native
  def objectStore[Store <: IDBObjectStore](name: api.IDBObjectStore.Name): Store = js.native
}

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
