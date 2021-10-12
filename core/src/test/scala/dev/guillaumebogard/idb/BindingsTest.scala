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
