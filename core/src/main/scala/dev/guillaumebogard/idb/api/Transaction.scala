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
type Transaction[T] = Free[TransactionA, T]

enum TransactionA[T]:
  case Add(store: ObjectStore.Name, value: js.Any, key: Option[Key]) extends TransactionA[Key]
  case Delete(store: ObjectStore.Name, keyOrKeyRange: Key | KeyRange) extends TransactionA[Unit]
  case Put(store: ObjectStore.Name, value: js.Any, key: Option[Key]) extends TransactionA[Key]
  case Get(store: ObjectStore.Name, key: Key) extends TransactionA[Option[js.Any]]
  case GetAll(store: ObjectStore.Name, keyRange: Option[KeyRange], count: Int)
      extends TransactionA[Seq[js.Any]]

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

  def get(store: ObjectStore.Name, key: Key): Transaction[Option[js.Any]] =
    Free.liftF(TransactionA.Get(store, key))
  def put(store: ObjectStore.Name, value: js.Any, key: Option[Key]) =
    Free.liftF(TransactionA.Put(store, value, key))
  def add(store: ObjectStore.Name, value: js.Any, key: Option[Key]) =
    Free.liftF(TransactionA.Add(store, value, key))
  def getAll(store: ObjectStore.Name, range: Option[KeyRange] = None, count: Int = 0) =
    Free.liftF(TransactionA.GetAll(store, range, count))
  def delete(store: ObjectStore.Name, keyOrKeyRange: Key | KeyRange) =
    Free.liftF(TransactionA.Delete(store, keyOrKeyRange))
