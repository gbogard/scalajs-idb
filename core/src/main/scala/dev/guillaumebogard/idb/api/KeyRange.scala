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
import dev.guillaumebogard.idb.internal.lowlevel

@js.native
trait KeyRange extends js.Object

object KeyRange:
  def lowerBound(lowerBound: Key, lowerOpen: Boolean = false): KeyRange =
    lowlevel.idbKeyRange.lowerBound(lowerBound, lowerOpen)
  def upperBound(upperBound: Key, upperOpen: Boolean = false): KeyRange =
    lowlevel.idbKeyRange.upperBound(upperBound, upperOpen)
  def bound(lowerBound: Key, upperBound: Key, lowerOpen: Boolean = false, upperOpen: Boolean = false): KeyRange =
    lowlevel.idbKeyRange.bound(lowerBound, upperBound, lowerOpen, upperOpen)
  def only(key: Key): KeyRange = lowlevel.idbKeyRange.only(key)
