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

package dev.guillaumebogard.idb.api

import scala.collection.mutable
import scalajs.js
import js.JSConverters.*
import dev.guillaumebogard.idb.internal.lowlevel.IDBObjectStore.CreateObjectStoreOptions

trait ObjectStore[T]:
  val name: ObjectStore.Name

  def get(key: Key)(using decoder: Decoder[T]): Transaction[Option[T]] =
    Transaction.get(name, key).map(_.map(v => decoder.decode(v)))

  def getAll(range: Option[KeyRange] = None, count: Int = 0)(using
      decoder: Decoder[T]
  ): Transaction[Seq[T]] =
    Transaction.getAll(name, range, count).map(_.map(v => decoder.decode(v)).toSeq)

  def getAll(range: KeyRange)(using Decoder[T]): Transaction[Seq[T]] = getAll(Some(range))

  def getAll(lowerBound: Key, upperBound: Key)(using Decoder[T]): Transaction[Seq[T]] = getAll(
    KeyRange.bound(lowerBound, upperBound)
  )

  def delete(key: Key | KeyRange): Transaction[Unit] = Transaction.delete(name, key)

object ObjectStore:
  opaque type Name = String
  object Name:
    def apply(name: String): Name = name

  def apply[T](name: String): ObjectStoreWithOutOfLineKeys[T] = withOutOfLineKeys[T](name)

  def withOutOfLineKeys[T](objectStoreName: String): ObjectStoreWithOutOfLineKeys[T] =
    new ObjectStoreWithOutOfLineKeys:
      val name = Name(objectStoreName)

  def withInlineKeys[T](
      objectStoreName: String,
      keypath: KeyPath,
      autoIncrement: Boolean = false
  ): ObjectStoreWithInlineKeys[T] =
    new ObjectStoreWithInlineKeys:
      val name = Name(objectStoreName)
      val options = CreateObjectStoreOptions(keypath, autoIncrement)

/** A [[ObjectStoreWithInlineKeys]] is an object store whose keys can be automatically calculated,
  * either from a generator or by fetching a [[KeyPath]] on the inserted objects. It means that the
  * [[put]] and [[add]] operations for example don't require you to provide an explicit key. To
  * insert a value inside a [[ObjectStoreWithInlineKeys]], you need an [[ObjectEncoder]].
  */
trait ObjectStoreWithInlineKeys[T] extends ObjectStore[T]:
  val options: CreateObjectStoreOptions

  def put(value: T)(using encoder: ObjectEncoder[T]) =
    Transaction.put(name, encoder.encode(value), None)
  def add(value: T)(using encoder: ObjectEncoder[T]) =
    Transaction.add(name, encoder.encode(value), None)

/** A [[ObjectStoreWithOutOfLineKeys]] is an object store whose keys are explicitly provided by you
  * when you insert records in it. To insert a value inside a [[ObjectStoreWithOutOfLineKeys]], you
  * need a simple[[Encoder]].
  */
trait ObjectStoreWithOutOfLineKeys[T] extends ObjectStore[T]:

  def put(value: T, key: Key)(using encoder: Encoder[T]) =
    Transaction.put(name, encoder.encode(value), Some(key))
  def add(value: T, key: Key)(using encoder: Encoder[T]) =
    Transaction.add(name, encoder.encode(value), Some(key))
