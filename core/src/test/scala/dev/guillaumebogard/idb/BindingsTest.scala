package dev.guillaumebogard.idb

import utest._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import dev.guillaumebogard.idb.internal._
import dev.guillaumebogard.idb.api._
import scala.concurrent.Promise

object BindingTest extends TestSuite {
  val tests = Tests {
    test("IDBFactory") {
      test("opening a database should succeed") {
        indexedDB.openFuture(IDBDatabase.Name("test")).success
      }
    }

    test("IDBDatabase") {
      val dbName = IDBDatabase.Name("test2")
      val championsStoreName = IDBObjectStore.Name("champions")
      val key = Key("favChamp")
      val value: js.Any = "Illaoi"

      test("creating an object store without options, creating and retrieving a key") {
        for {
          db <- indexedDB
            .openFuture(
              dbName,
              onUpgradeNeeded = event => {
                event.target.result.createObjectStore(championsStoreName)
              }
            )
            .success
          tx = db.transaction(IDBTransaction.Mode.ReadWrite, championsStoreName)
          store = tx.objectStore[IDBObjectStore.WithOutOfLineKey](championsStoreName)
          insertRes <- store.add(value, key).future
          getRes <- store.get(key).future
        } yield
          assert(insertRes == key)
          assert(getRes == value)
      }
    }
  }
}
