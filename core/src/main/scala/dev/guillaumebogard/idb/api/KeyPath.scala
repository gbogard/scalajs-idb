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
import js.JSConverters._

enum KeyPath:
  case Empty
  case Identifier(id: String)
  case Path(ids: Seq[Identifier])

object KeyPath:
  opaque type JS = js.Array[String] | Null

  given Conversion[KeyPath, JS] = toJS(_)

  def fromJS(input: JS): KeyPath = input match
    case arr: js.Array[String] => Path(arr.map(Identifier(_): Identifier).toSeq)
    case null => Empty

  extension (keyPath: KeyPath)
    def toJS: KeyPath.JS = keyPath match
      case Empty => null
      case Identifier(id) => js.Array(id)
      case Path(ids) => ids.toJSArray.map(_.id)
