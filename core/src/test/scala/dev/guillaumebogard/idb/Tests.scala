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
import dev.guillaumebogard.idb.api.*
import scala.concurrent.{Future, ExecutionContext}
import cats.implicits.given
import cats.data.NonEmptyList

given ec: ExecutionContext = ExecutionContext.global

class TestsUsingBackend[F[_]](toFuture: [A] => F[A] => Future[A])(using backend: Backend[F])
    extends TestSuite {
  import backend.given

  val tests = Tests {
    test("Simple put and get test") {
      val championsStoreName = ObjectStore.Name("champions")
      val dbName = Database.Name("test")
      val schema = Schema().createObjectStore(championsStoreName)
      val key = Key("favChamp")

      toFuture {
        Database
          .open[F](dbName, schema)
          .rethrow
          .flatMap(_.readWrite(NonEmptyList.of(championsStoreName)) {
            for {
              heroes <- Transaction.getObjectStore(championsStoreName)
              _ <- heroes.put("Illaoi", Some(key))
              illaoi <- heroes.get(key)
              _ = {
                println("+++++")
                println(illaoi)
              }
            } yield assert(illaoi == Some("Illaoi"))
          })
      }
    }
  }
}

object FutureTests extends TestsUsingBackend[Future]([A] => (fa: Future[A]) => fa)
