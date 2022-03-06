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
import utest.*

object CodecTests extends TestSuite:

  case class User(id: Int, name: String, metadata: Map[String, String] = Map.empty)
      derives ObjectEncoder,
        Decoder
  case class TodoListItem(text: String, isDone: Boolean) derives ObjectEncoder, Decoder
  case class TodoList(title: String, items: List[TodoListItem]) derives ObjectEncoder, Decoder

  enum UserWithRole derives ObjectEncoder, Decoder:
    case Admin(user: User)
    case Editor(user: User)
    case Nested(users: List[UserWithRole])

  enum MyBool derives ObjectEncoder, Decoder:
    case True
    case False

  // Example data types backported from gbogard/boardgames since
  // the initially had an issue with infinite recursion at runtime.
  final case class Player(name: String, color: Color) derives ObjectEncoder, Decoder:
    val id: PlayerId = name

  enum Color(val hex: String) derives ObjectEncoder, Decoder:
    case Green extends Color("#78e08f")
    case Yellow extends Color("#f6b93b")
    case Blue extends Color("#1e3799")
    case Red extends Color("#b71540")
    case CustomColor(override val hex: String) extends Color(hex)

  enum GameState derives ObjectEncoder, Decoder:
    case Pending
    case Finished

  enum GameType:
    case SevenWonders

  opaque type GameId = Int

  object GameId:
    given Encoder[GameId] = Encoder.int
    given Decoder[GameId] = Decoder.int

  opaque type PlayerId = String

  object PlayerId:
    given Encoder[PlayerId] = Encoder.string
    given Decoder[PlayerId] = Decoder.string
    given ObjectKeyEncoder[PlayerId] = ObjectKeyEncoder.string
    given ObjectKeyDecoder[PlayerId] = ObjectKeyDecoder.string
  end PlayerId

  final case class Game(
      id: GameId,
      state: GameState = GameState.Pending,
      players: Map[PlayerId, Player] = Map.empty
  ) derives ObjectEncoder,
        Decoder

  object Game:
    def apply(players: List[Player]): Game =
      Game(1, players = players.map(p => (p.id, p)).toMap)

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

    test("Game") {
      val players = (1 to 5).map(i => Player(s"player-$i", Color.Blue)).toList
      val game = Game(players)
      doTest(game)
    }
  }

  private val compareMyBool: (MyBool, MyBool) => Boolean = (a, b) =>
    (a, b) match
      case (MyBool.True, MyBool.True)   => true
      case (MyBool.False, MyBool.False) => true
      case _                            => false

  private def doTest[T: Encoder: Decoder](
      value: T,
      isEqual: (T, T) => Boolean = (a: T, b: T) => a == b
  ) =
    assert(isEqual(Decoder[T].decode(Encoder[T].encode(value)), value))
