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
  def bound(lowerBound: Key, upperBound: Key, lowerOpen: Boolean = false, upperOpen: false): KeyRange =
    lowlevel.idbKeyRange.bound(lowerBound, upperBound, lowerOpen, upperOpen)
  def only(key: Key): KeyRange = lowlevel.idbKeyRange.only(key)
