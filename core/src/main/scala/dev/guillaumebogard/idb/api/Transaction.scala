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

import scala.scalajs.js
import cats.free.Free

trait ObjectStore:
  protected val name: ObjectStore.Name

  def get(key: Key): Transaction[Option[js.Object]] = Transaction.get(name, key)
  def put(value: js.Any, key: Option[Key]) = Transaction.put(name, value, key)

object ObjectStore:
  opaque type Name = String
  object Name:
    def apply(name: String): Name = name

type Transaction[T] = Free[TransactionA, T]

enum TransactionA[T]:
  case GetObjectStore(name: ObjectStore.Name) extends TransactionA[ObjectStore]
  case Put(store: ObjectStore.Name, value: js.Any, key: Key | Null) extends TransactionA[Unit]
  case Get(store: ObjectStore.Name, key: Key) extends TransactionA[Option[js.Object]]

object Transaction:
  enum Mode:
    case ReadWrite, ReadOnly, ReadWriteFlush

  object Mode:
    opaque type JS = String
    extension (mode: Mode)
      def toJS: JS =
        mode match
          case ReadOnly       => "readonly"
          case ReadWrite      => "readwrite"
          case ReadWriteFlush => "readwriteflush"
    given Conversion[Mode, JS] = toJS(_)

  def getObjectStore(name: ObjectStore.Name): Transaction[ObjectStore] =
    Free.liftF(TransactionA.GetObjectStore(name))

  def get(store: ObjectStore.Name, key: Key): Transaction[Option[js.Object]] =
    Free.liftF(TransactionA.Get(store, key))
  def put(store: ObjectStore.Name, value: js.Any, key: Option[Key]) =
    Free.liftF(TransactionA.Put(store, value, key.orNull))
