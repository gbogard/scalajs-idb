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
 */

package dev.guillaumebogard.idb.internal.lowlevel

import scala.scalajs.js
import dev.guillaumebogard.idb.*
import dev.guillaumebogard.idb.api.*
import scala.scalajs.js.annotation.*
import js.JSConverters.*

@js.native
@JSGlobal("indexedDB")
private[internal] val indexedDB: IDBFactory = js.native

@js.native
trait IDBFactory extends js.Object:
  def open(name: api.Database.Name, version: Int): IDBOpenDBRequest

@js.native
@JSGlobal("IDBKeyRange")
val idbKeyRange: IDBKeyRangeFactory = js.native

@js.native
trait IDBKeyRangeFactory extends js.Object:
  def lowerBound(lowerBound: Key, lowerOpen: Boolean = false): KeyRange
  def upperBound(upperBound: Key, upperOpen: Boolean = false): KeyRange
  def bound(
      lowerBound: Key,
      upperBound: Key,
      lowerOpen: Boolean = false,
      upperOpen: Boolean = false
  ): KeyRange
  def only(key: Key): KeyRange

@js.native
private[internal] trait IDBDatabase extends js.Object:
  val name: String = js.native
  val version: Int = js.native

  def close(): Unit = js.native

  def createObjectStore(
      name: ObjectStore.Name
  ): IDBObjectStore = js.native

  def createObjectStore(
      name: ObjectStore.Name,
      options: IDBObjectStore.CreateObjectStoreOptions
  ): IDBObjectStore = js.native

  def transaction(
      stores: js.Array[ObjectStore.Name],
      mode: api.Transaction.Mode.JS
  ): IDBTransaction = js.native

@js.native
private[internal] trait IDBRequest[Target, Result] extends js.Object:
  def result: Result | Null = js.native
  def error: DOMException | Null = js.native

  var onsuccess: js.Function1[DOMEvent[Target], Unit] = js.native
  var onerror: js.Function1[DOMEvent[Unit], Unit] = js.native

  @js.native
  trait Completed extends IDBRequest[Target, Result]:
    override def result: Result = js.native

  @js.native
  trait Errored extends IDBRequest[Target, Result]:
    override def error: DOMException = js.native

@js.native
private[internal] trait IDBOpenDBRequest extends IDBRequest[IDBOpenDBRequest, IDBDatabase]:
  var onupgradeneeded: js.Function1[UpgradeNeededEvent, Unit] = js.native
  var onblocked: js.Function1[DOMEvent[IDBOpenDBRequest#Completed], Unit] = js.native

@js.native
private[internal] trait AddRequest extends IDBRequest[AddRequest, Key]

@js.native
private[internal] trait IDBObjectStore extends js.Object:
  def get(key: Key): IDBRequest[Unit, js.UndefOr[js.Any]] = js.native
  def delete(keyOrKeyRange: Key | KeyRange): IDBRequest[Unit, Unit] = js.native
  def getAll(range: KeyRange | Null, count: Int): IDBRequest[Unit, js.Array[js.Any]] = js.native
  def add[Value <: js.Any](value: Value, key: js.UndefOr[Key]): AddRequest = js.native
  def put[Value <: js.Any](value: Value, key: js.UndefOr[Key]): AddRequest = js.native

object IDBObjectStore:
  @JSExportAll
  case class CreateObjectStoreOptions(
      keyPath: KeyPath.JS,
      autoIncrement: js.UndefOr[Boolean]
  )

@js.native
private[internal] trait IDBTransaction extends js.Object:
  val db: IDBDatabase = js.native
  val mode: api.Transaction.Mode.JS = js.native
  def objectStore(name: ObjectStore.Name): IDBObjectStore = js.native

@js.native
private[internal] trait DOMException extends js.Error

@js.native
private[internal] trait DOMEvent[Target] extends js.Object:
  val target: Target = js.native

@js.native
private[internal] trait UpgradeNeededEvent extends DOMEvent[IDBOpenDBRequest#Completed]:
  val oldVersion: Int = js.native
  val newVersion: Int = js.native
