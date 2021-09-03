package dev.guillaumebogard.idb

import utest._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import dev.guillaumebogard.idb.internal._
import dev.guillaumebogard.idb.internal.FutureAPI._
import scala.concurrent.Promise

object BindingTest extends TestSuite {
  val tests = Tests {
    test("IDBFactory") {
      test("opening a database should succeed") {
        indexedDB.openFuture("test").open
      }
    }

    test("IDBDatabase") {
      test("creating an object store without options") {
        indexedDB
          .openFuture(
            "test2",
            onUpgradeNeeded = event => {
              event.target.result.createObjectStore("champions")
            }
          )
          .open
      }
    }
  }
}
