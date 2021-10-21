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

import cats.implicits.*
import dev.guillaumebogard.idb.api.*
import dev.guillaumebogard.idb.api.ObjectEncoder.given
import scala.scalajs.js
import utest._

object CodecTests extends TestSuite {

  case class User(id: Int, name: String, metadata: Map[String, String] = Map.empty) derives ObjectEncoder, Decoder
  case class TodoListItem(text: String, isDone: Boolean) derives ObjectEncoder, Decoder
  case class TodoList(title: String, items: List[TodoListItem]) derives ObjectEncoder, Decoder

  enum UserWithRole derives ObjectEncoder, Decoder:
    case Admin(user: User)
    case Editor(user: User)
    case Nested(users: List[UserWithRole])

  enum Color(val value: String) derives ObjectEncoder, Decoder:
    case Red extends Color("red")
    case Blue extends Color("blue")
    case CustomColor(override val value: String) extends Color(value)

  enum MyBool derives ObjectEncoder, Decoder:
    case True
    case False

  val tests = Tests {
    test("Simple case class") {
      doTest(User(43, "John Doe"))
    }

    test("Nested case class") {
      doTest(List.fill(2)(TodoList("Test", List.fill(2)(TodoListItem("test", false)))))
    }

    test("Map") {
      val map: Map[Int, User] =
        (1 to 5).map(i => (i, User(i, s"User $i", Map("birthDate" -> "03/10/1986")))).toMap
      doTest(map)
    }

    test("ADT") {
      val users = List[UserWithRole](
        UserWithRole.Admin(User(1, "Test")),
        UserWithRole.Editor(User(2, "Test")),
        UserWithRole.Editor(User(3, "Test")),
        UserWithRole.Nested(
          List(
            UserWithRole.Admin(User(10, "Test")),
            UserWithRole.Editor(User(20, "Test"))
          )
        )
      )
      val colors = List(Color.Red, Color.CustomColor("teal"), Color.Blue)
      doTest(users)
      doTest(colors)
      doTest(MyBool.True, compareMyBool)
      doTest(MyBool.False, compareMyBool)
    }
  }

  private val compareMyBool: (MyBool, MyBool) => Boolean = (a, b) =>
    (a, b) match
      case (MyBool.True, MyBool.True)   => true
      case (MyBool.False, MyBool.False) => true
      case _              => false

  private def doTest[T: Encoder: Decoder](value: T, isEqual: (T, T) => Boolean = (a: T, b: T) => a == b) =
    assert(isEqual(Decoder[T].decode(Encoder[T].encode(value)), value))
}
