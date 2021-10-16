package dev.guillaumebogard.idb.api

import scalajs.js

trait ObjectStore:
  protected val name: ObjectStore.Name
  def get(key: Key): Transaction[Option[js.Any]] = Transaction.get(name, key)
  def put(value: js.Any, key: Option[Key]) = Transaction.put(name, value, key)
  def add(value: js.Any, key: Option[Key]) = Transaction.add(name, value, key)

object ObjectStore:
  opaque type Name = String
  object Name:
    def apply(name: String): Name = name
