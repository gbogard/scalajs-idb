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
import scala.scalajs.js
import dev.guillaumebogard.idb.internal._
import dev.guillaumebogard.idb.api._
import scala.concurrent.{Future, ExecutionContext}
import cats.implicits.given

given ec: ExecutionContext = ExecutionContext.global

object BindingTest extends TestSuite {

  val tests = Tests {
    test("Simple get and put test") {
      val dbName = Database.Name("test")
      val championsStoreName = ObjectStore.Name("champions")
      val key = Key("favChamp")
      val value: js.Any = "Illaoi"

      Database
        .open[Future](dbName, Schema())
        .rethrow
        .flatMap(_.transact(Transaction.Mode.ReadWrite) {
          for {
            heroes <- Transaction.getObjectStore(championsStoreName)
            _ <- heroes.put(value, Some(key))
            illaoi <- heroes.get(key)
          } yield assert(illaoi == value)
        })
    }
  }
}
