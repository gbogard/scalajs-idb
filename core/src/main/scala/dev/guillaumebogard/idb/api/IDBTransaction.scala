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

trait IDBTransaction[F[_]]

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

