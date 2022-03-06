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

package dev.guillaumebogard.idb.api

import cats.*
import cats.implicits.given
import dev.guillaumebogard.idb.internal.*
import scala.collection.mutable
import scala.compiletime.*
import scala.deriving.Mirror
import scala.util.*
import scalajs.js
import js.JSConverters.*

/** A type class describing the ability to turn Scala types into Javascript types, so they can
  * stored in an object store.
  */
trait Encoder[T]:
  def encode(value: T): js.Any

object Encoder:
  def apply[T](using encoder: Encoder[T]): Encoder[T] = encoder
  def from[T](toJS: T => js.Any): Encoder[T] = value => toJS(value)

  private def cast[T]: Encoder[T] = from[T](_.asInstanceOf[js.Any])

  given string: Encoder[String] = cast[String]
  given int: Encoder[Int] = cast[Int]
  given float: Encoder[Float] = cast[Float]
  given double: Encoder[Double] = cast[Double]
  given boolean: Encoder[Boolean] = cast[Boolean]

  /** An instance of [[Encoder]] provided for all subtypes of [[js.Any]]
    */
  given jsAny[T <: js.Any]: Encoder[T] = cast[T]

  given seq[T, S <: Seq[T]](using encoder: Encoder[T]): Encoder[S] = _.map(encoder.encode).toJSArray

  given option[T](using encoder: Encoder[T]): Encoder[Option[T]] =
    case None    => null.asInstanceOf[js.Any]
    case Some(v) => encoder.encode(v)

  given map[K: ObjectKeyEncoder, V: Encoder]: ObjectEncoder[Map[K, V]] = ObjectEncoder.map

  given Contravariant[Encoder] with
    def contramap[A, B](fa: Encoder[A])(f: B => A): Encoder[B] = (value: B) => fa.encode(f(value))

/** A type class describing the ability to turn Javascript types into Scala types so they can be
  * retrieved from an object store.
  */
trait Decoder[T]:
  def decode(value: js.Any): T

object Decoder extends DecoderDerivation:
  def apply[T](using decoder: Decoder[T]): Decoder[T] = decoder
  def from[T](fromJS: js.Any => T): Decoder[T] = value => fromJS(value)
  private def cast[T]: Decoder[T] = from[T](_.asInstanceOf[T])

  given string: Decoder[String] = cast[String]
  given int: Decoder[Int] = cast[Int]
  given long: Decoder[Long] = cast[Long]
  given float: Decoder[Float] = cast[Float]
  given double: Decoder[Double] = cast[Double]
  given boolean: Decoder[Boolean] = cast[Boolean]

  /** An instance of [[Decoder]] provided for all subtypes of [[js.Any]]
    */
  given jsAny[T <: js.Any]: Decoder[T] = cast[T]

  given option[T](using decoder: Decoder[T]): Decoder[Option[T]] =
    value => Option(value).map(decoder.decode)

  given seq[T](using decoder: Decoder[T]): Decoder[Seq[T]] =
    Decoder[js.Array[js.Any]].map(_.map(decoder.decode).toSeq)

  given list[T](using decoder: Decoder[T]): Decoder[List[T]] = seq.map(_.toList)

  given vector[T](using decoder: Decoder[T]): Decoder[Vector[T]] = seq.map(_.toVector)

  given map[K, V](using keyDecoder: ObjectKeyDecoder[K], decoder: Decoder[V]): Decoder[Map[K, V]] =
    Decoder
      .jsAny[js.Dictionary[js.Any]]
      .map(_.toMap.map((k: String, v: js.Any) => (keyDecoder.fromString(k), decoder.decode(v))))

  given Functor[Decoder] with
    def map[A, B](fa: Decoder[A])(f: A => B): Decoder[B] = (jsValue: js.Any) =>
      f(fa.decode(jsValue))

/** A type class describing the ability to turn Scala types into Javascript Objects. Similar to
  * [[Encoder]], except the Scala values are translated specifically to objects, not just any
  * Javascript type. This provides additional guarantees for object stores with in-line keys.
  */
trait ObjectEncoder[T] extends Encoder[T]:
  def encode(value: T): js.Any = toObject(value)
  def toObject(value: T): js.Object

object ObjectEncoder extends EncoderDerivation:
  def apply[T](using encoder: ObjectEncoder[T]): ObjectEncoder[T] = encoder
  def from[T](toJS: T => js.Object): ObjectEncoder[T] = value => toJS(value)

  /** An instance of [[ObjectEncoder]] provided for subtypes of [[js.Object]]
    */
  def castToObject[T <: js.Object]: ObjectEncoder[T] = v => v.asInstanceOf[js.Object]

  given map[K, V](using
      keyEncoder: ObjectKeyEncoder[K],
      encoder: Encoder[V]
  ): ObjectEncoder[Map[K, V]] with
    def toObject(map: Map[K, V]) =
      map
        .map((k, v) => (keyEncoder.toKey(k), encoder.encode(v)))
        .toJSDictionary
        .asInstanceOf[js.Object]

  given Contravariant[ObjectEncoder] with
    def contramap[A, B](fa: ObjectEncoder[A])(f: B => A): ObjectEncoder[B] = (value: B) =>
      fa.toObject(f(value))

trait ObjectKeyEncoder[T]:
  def toKey(value: T): String

object ObjectKeyEncoder:
  def apply[T](using ke: ObjectKeyEncoder[T]): ObjectKeyEncoder[T] = ke
  def from[T](toKey: T => String): ObjectKeyEncoder[T] = value => toKey(value)

  given string: ObjectKeyEncoder[String] with
    def toKey(str: String): String = str

  given int: ObjectKeyEncoder[Int] = string.contramap(_.toString)
  given float: ObjectKeyEncoder[Float] = string.contramap(_.toString)
  given double: ObjectKeyEncoder[Double] = string.contramap(_.toString)

  given Contravariant[ObjectKeyEncoder] with
    def contramap[A, B](fa: ObjectKeyEncoder[A])(f: B => A): ObjectKeyEncoder[B] =
      value => fa.toKey(f(value))

trait ObjectKeyDecoder[T]:
  def fromString(key: String): T

object ObjectKeyDecoder:
  def apply[T](using kd: ObjectKeyDecoder[T]): ObjectKeyDecoder[T] = kd
  def from[T](fromString: String => T): ObjectKeyDecoder[T] = key => fromString(key)

  given string: ObjectKeyDecoder[String] with
    def fromString(key: String): String = key
  given int: ObjectKeyDecoder[Int] = string.map(_.toInt)
  given float: ObjectKeyDecoder[Float] = string.map(_.toFloat)
  given long: ObjectKeyDecoder[Long] = string.map(_.toLong)
  given double: ObjectKeyDecoder[Double] = string.map(_.toDouble)

  given Functor[ObjectKeyDecoder] with
    def map[A, B](fa: ObjectKeyDecoder[A])(f: A => B): ObjectKeyDecoder[B] =
      key => f(fa.fromString(key))
