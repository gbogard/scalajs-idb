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

import scalajs.js
import dev.guillaumebogard.idb.internal.lowlevel.IDBObjectStore.CreateObjectStoreOptions

trait ObjectStore[T]:
  val name: ObjectStore.Name
  def get(key: Key): Transaction[Option[T]]
  def getAll(range: Option[KeyRange] = None, count: Int = 0): Transaction[Seq[T]]
  def getAll(range: KeyRange): Transaction[Seq[T]] = getAll(Some(range))
  def getAll(lowerBound: Key, upperBound: Key): Transaction[Seq[T]] = getAll(KeyRange.bound(lowerBound, upperBound))

object ObjectStore:
  opaque type Name = String
  object Name:
    def apply(name: String): Name = name

  def apply[T: Codec](name: String): ObjectStoreWithOutOfLineKeys[T] = withOutOfLineKeys[T](name)

  def withOutOfLineKeys[T: Codec](objectStoreName: String): ObjectStoreWithOutOfLineKeys[T] =
    new ObjectStoreWithOutOfLineKeys:
      val name = Name(objectStoreName)
      val codec = Codec[T]

  def withInlineKeys[T: ObjectCodec](
      objectStoreName: String,
      keypath: KeyPath,
      autoIncrement: Boolean = false
  ): ObjectStoreWithInlineKeys[T] =
    new ObjectStoreWithInlineKeys:
      val name = Name(objectStoreName)
      val codec = ObjectCodec[T]
      val options = CreateObjectStoreOptions(keypath, autoIncrement)

/** A [[ObjectStoreWithInlineKeys]] is an object store whose keys can be automatically calculated, either from
  * a generator or by fetching a [[KeyPath]] on the inserted objects. It means that the [[put]] and [[add]]
  * operations for example don't require you to provide an explicit key. To insert a value inside a
  * [[ObjectStoreWithInlineKeys]], you need an [[ObjectCodec]].
  */
trait ObjectStoreWithInlineKeys[T] extends ObjectStore[T]:
  protected val codec: ObjectCodec[T]
  val options: CreateObjectStoreOptions

  def get(key: Key): Transaction[Option[T]] =
    Transaction.get(name, key).map(_.map(v => codec.decode(v.asInstanceOf[js.Object])))
  def getAll(range: Option[KeyRange] = None, count: Int = 0): Transaction[Seq[T]] =
    Transaction.getAll(name, range, count).map(_.map(v => codec.decode(v.asInstanceOf[js.Object])).toSeq)
  def put(value: T) = Transaction.put(name, codec.encode(value), None)
  def add(value: T) = Transaction.add(name, codec.encode(value), None)

/** A [[ObjectStoreWithOutOfLineKeys]] is an object store whose keys are explicitly provided by you when you
  * insert records in it. To insert a value inside a [[ObjectStoreWithOutOfLineKeys]], you need a [[Codec]].
  */
trait ObjectStoreWithOutOfLineKeys[T] extends ObjectStore[T]:
  protected val codec: Codec[T]

  def get(key: Key): Transaction[Option[T]] =
    Transaction.get(name, key).map(_.map(codec.decode))
  def getAll(range: Option[KeyRange] = None, count: Int = 0): Transaction[Seq[T]] =
    Transaction.getAll(name, range, count).map(_.map(v => codec.decode(v.asInstanceOf[js.Any])).toSeq)
  def put(value: T, key: Key) = Transaction.put(name, codec.encode(value), Some(key))
  def add(value: T, key: Key) = Transaction.add(name, codec.encode(value), Some(key))

/** A [[Codec]] is a type-class providing the ability to turn Scala types into Javascript types for insertion
  * into an [[ObjectStore]] and Javascript types into Scala types for retrieval from an [[ObjectStore]].
  */
trait Codec[T]:
  def encode(value: T): js.Any
  def decode(value: js.Any): T

object Codec:
  def apply[T](using codec: Codec[T]): Codec[T] = codec
  def from[T](toJS: T => js.Any, fromJS: js.Any => T): Codec[T] =
    new Codec[T]:
      def encode(value: T) = toJS(value)
      def decode(value: js.Any) = fromJS(value)

  given [T <: js.Any]: Codec[T] = Codec.from[T](_.asInstanceOf[js.Any], _.asInstanceOf[T])

trait ObjectCodec[T]:
  def encode(value: T): js.Object
  def decode(value: js.Object): T

object ObjectCodec:
  def apply[T](using codec: ObjectCodec[T]): ObjectCodec[T] = codec
  def from[T](toJS: T => js.Object, fromJS: js.Object => T): ObjectCodec[T] =
    new ObjectCodec[T]:
      def encode(value: T) = toJS(value)
      def decode(value: js.Object) = fromJS(value)

  given [T <: js.Object]: ObjectCodec[T] = ObjectCodec.from[T](_.asInstanceOf[js.Object], _.asInstanceOf[T])
