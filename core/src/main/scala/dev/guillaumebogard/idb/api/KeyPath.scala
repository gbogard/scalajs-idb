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
import js.JSConverters.*

enum KeyPath:
  case Empty
  case Identifier(id: String)
  case Path(ids: Seq[Identifier])

extension (path: KeyPath)
  def add(identifier: String): KeyPath =
    path match
      case KeyPath.Empty             => KeyPath.Identifier(identifier)
      case other: KeyPath.Identifier => KeyPath.Path(Seq(other, KeyPath.Identifier(identifier)))
      case KeyPath.Path(others)      => KeyPath.Path(others :+ KeyPath.Identifier(identifier))
  def /(identifier: String): KeyPath = add(identifier)

object KeyPath:
  opaque type JS = String | Null
  given Conversion[KeyPath, JS] = toJS(_)

  def apply(): KeyPath = KeyPath.Empty
  def apply(identifier: String): KeyPath = KeyPath.Identifier(identifier)

  def fromJS(input: JS): KeyPath = input match
    case str: String => Path(str.split(".").nn.map(str => Identifier(str.nn): Identifier).toSeq)
    case null        => Empty

  extension (keyPath: KeyPath)
    def toJS: KeyPath.JS = keyPath match
      case Empty          => null
      case Identifier(id) => id
      case Path(ids)      => ids.mkString(".")
