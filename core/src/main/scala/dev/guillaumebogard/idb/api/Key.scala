package dev.guillaumebogard.idb.api

opaque type Key = String

extension (key: Key) def toString: String = key

object Key:
  def apply(key: String): Key = key
