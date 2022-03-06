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

import cats.*
import cats.data.NonEmptyList
import cats.implicits.given
import dev.guillaumebogard.idb.api.*
import scala.concurrent.{Future, ExecutionContext}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll
import utest.*

given ec: ExecutionContext = ExecutionContext.global

class Champion(val name: String, val position: String) extends js.Object

given Eq[Champion] = Eq.by(c => c.name.hashCode * c.position.hashCode)

val championsStore = ObjectStore[Champion]("champions")

case class User(id: Int, name: String) derives ObjectEncoder, Decoder
val usersStore = ObjectStore.withInlineKeys[User]("users", KeyPath("id"))

class TestsUsingBackend[F[_]: Backend: Monad](toFuture: [A] => F[A] => Future[A]) extends TestSuite:
  val tests = Tests {
    test("ChampionsStore (Out-of-line keys)") {
      test("Simple put and get") {
        val dbName = Database.Name("champions-store-tests")
        val schema = Schema().createObjectStore(championsStore)
        val illaoi = Champion("Illaoi", "top")
        val key = Key("favChamp")

        toFuture {
          Database
            .open[F](dbName, schema)
            .flatMap(_.readWrite(NonEmptyList.of(championsStore.name)) {
              for
                _ <- championsStore.put(illaoi, key)
                result <- championsStore.get(key)
              yield assert(result === Some(illaoi))
            })
        }
      }
    }

    test("User store (inline keys)") {
      val dbName = Database.Name("users-store-tests")
      val schema = Schema().createObjectStore(usersStore)

      test("Simple put and get") {
        val johnDoe = User(1, "John Doe")
        toFuture {
          Database
            .open[F](dbName, schema)
            .flatMap(_.readWrite(NonEmptyList.of(usersStore.name)) {
              for
                insertedKey <- usersStore.put(johnDoe)
                result <- usersStore.get(insertedKey)
              yield assert(result == Some(johnDoe) && insertedKey == johnDoe.id.toKey)
            })
        }
      }

      test("Multiple puts and getAll") {
        val users = List(
          User(1, "Paul"),
          User(2, "John"),
          User(3, "George"),
          User(4, "Ringo")
        )
        toFuture {
          Database
            .open[F](dbName, schema)
            .flatMap(_.readWrite(NonEmptyList.of(usersStore.name)) {
              for
                _ <- users.traverse_(usersStore.put)
                completeDataset <- usersStore.getAll()
                partialDataset <- usersStore.getAll(KeyRange.bound(2.toKey, 3.toKey))
              yield assert(
                completeDataset.toList == users && partialDataset == users.drop(1).take(2)
              )
            })
        }
      }

      test("Delete single key") {
        val users = List(
          User(1, "Paul"),
          User(2, "John"),
          User(3, "George"),
          User(4, "Ringo")
        )
        toFuture {
          Database
            .open[F](dbName, schema)
            .flatMap(_.readWrite(NonEmptyList.of(usersStore.name)) {
              for
                _ <- users.traverse_(usersStore.put)
                dataset1 <- usersStore.getAll()
                _ <- usersStore.delete(Key(2))
                dataset2 <- usersStore.getAll()
              yield assert(
                dataset1 == users && dataset2 == users.filterNot(_.id == 2)
              )
            })
        }
      }

      test("Delete key range") {
        val users = List(
          User(1, "Paul"),
          User(2, "John"),
          User(3, "George"),
          User(4, "Ringo")
        )
        toFuture {
          Database
            .open[F](dbName, schema)
            .flatMap(_.readWrite(NonEmptyList.of(usersStore.name)) {
              for
                _ <- users.traverse_(usersStore.put)
                dataset1 <- usersStore.getAll()
                _ <- usersStore.delete(KeyRange.bound(2.toKey, 3.toKey))
                dataset2 <- usersStore.getAll()
              yield assert(
                dataset1 == users && dataset2 == List(
                  User(1, "Paul"),
                  User(4, "Ringo")
                )
              )
            })
        }
      }

    }
  }

object FutureTests extends TestsUsingBackend[Future]([A] => (fa: Future[A]) => fa)
