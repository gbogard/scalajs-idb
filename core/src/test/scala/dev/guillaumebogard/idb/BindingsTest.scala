package dev.guillaumebogard.idb

import utest._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import dev.guillaumebogard.idb.internal._
import scala.concurrent.Promise

object BindingTest extends TestSuite {
  val tests = Tests {
    test("IDBFactory") {
      test("opening a database should succeed") {
        indexedDb.openFuture("test").open
      }
    }
  }
}
