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

import dev.guillaumebogard.idb.api

type Schema = Schema.Schema

object Schema:
  opaque type Schema = Vector[SchemaOperation]

  def apply(): Schema = Vector.empty

  extension (schema: Schema)
    def createObjectStore[T](store: ObjectStore[T]): Schema =
      schema :+ SchemaOperation.CreateObjectStore(store)
    def lastVersion: Int = schema.length + 1
    def getPendingOperations(oldVersion: Int): Vector[SchemaOperation] =
      schema.drop(Math.max(0, oldVersion - 1))

enum SchemaOperation:
  case CreateObjectStore(store: ObjectStore[?])
