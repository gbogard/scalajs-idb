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

import scalajs.js
import scala.collection.mutable
import cats.*
import cats.implicits.given
import js.JSConverters.*
import scala.util.*
import scala.compiletime.*
import scala.deriving.Mirror

/** A type class describing the ability to turn Scala types into Javascript types, so they can stored in an
  * object store.
  */
trait Encoder[T]:
  def encode(value: T): js.Any

object Encoder:
  def apply[T](using encoder: Encoder[T]): Encoder[T] = encoder
  def from[T](toJS: T => js.Any): Encoder[T] = value => toJS(value)

  private def cast[T]: Encoder[T] = from[T](_.asInstanceOf[js.Any])

  given Encoder[String] = cast[String]
  given Encoder[Int] = cast[Int]
  given Encoder[Long] = cast[Long]
  given Encoder[Float] = cast[Float]
  given Encoder[Double] = cast[Double]
  given Encoder[Boolean] = cast[Boolean]

  /** An instance of [[Encoder]] provided for all subtypes of [[js.Any]]
    */
  given [T <: js.Any]: Encoder[T] = cast[T]

  given [T, S <: Seq[T]](using encoder: Encoder[T]): Encoder[S] = _.map(encoder.encode).toJSArray

  given Contravariant[Encoder] with
    def contramap[A, B](fa: Encoder[A])(f: B => A): Encoder[B] = (value: B) => fa.encode(f(value))

/** A type class describing the ability to turn Javascript types into Scala types so they can be retrieved
  * from an object store.
  */
trait Decoder[T]:
  def decode(value: js.Any): T

object Decoder:
  def apply[T](using decoder: Decoder[T]): Decoder[T] = decoder
  def from[T](fromJS: js.Any => T): Decoder[T] = value => fromJS(value)
  private def cast[T]: Decoder[T] = from[T](_.asInstanceOf[T])

  given Decoder[String] = cast[String]
  given Decoder[Int] = cast[Int]
  given Decoder[Long] = cast[Long]
  given Decoder[Float] = cast[Float]
  given Decoder[Double] = cast[Double]
  given Decoder[Boolean] = cast[Boolean]

  /** An instance of [[Decoder]] provided for all subtypes of [[js.Any]]
    */
  given [T <: js.Any]: Decoder[T] = cast[T]

  given [T](using decoder: Decoder[T]): Decoder[Seq[T]] =
    Decoder[js.Array[js.Any]].map(_.map(decoder.decode).toSeq)

  given [T](using decoder: Decoder[T]): Decoder[List[T]] = Decoder[Seq[T]].map(_.toList)

  given [T](using decoder: Decoder[T]): Decoder[Vector[T]] = Decoder[Seq[T]].map(_.toVector)

  given [K, V](using keyDecoder: ObjectKeyDecoder[K], decoder: Decoder[V]): Decoder[Map[K, V]] =
    Decoder[js.Dictionary[js.Any]]
      .map(_.toMap.map((k: String, v: js.Any) => (keyDecoder.fromString(k), decoder.decode(v))))

  given Functor[Decoder] with
    def map[A, B](fa: Decoder[A])(f: A => B): Decoder[B] = (jsValue: js.Any) => f(fa.decode(jsValue))


/** A type class describing the ability to turn Scala types into Javascript Objects. Similar to [[Encoder]],
  * except the Scala values are translated specifically to objects, not just any Javascript type. This
  * provides additional guarantees for object stores with in-line keys.
  */
trait ObjectEncoder[T] extends Encoder[T]:
  def encode(value: T): js.Any = toObject(value)
  def toObject(value: T): js.Object

object ObjectEncoder:
  def apply[T](using encoder: ObjectEncoder[T]): ObjectEncoder[T] = encoder
  def from[T](toJS: T => js.Object): ObjectEncoder[T] = value => toJS(value)

  /** An instance of [[ObjectEncoder]] provided for subtypes of [[js.Object]]
    */
  def castToObject[T <: js.Object]: ObjectEncoder[T] = v => v.asInstanceOf[js.Object]

  given [K, V](using keyEncoder: ObjectKeyEncoder[K], encoder: Encoder[V]): ObjectEncoder[Map[K, V]] with
    def toObject(map: Map[K, V]) =
      map.map((k, v) => (keyEncoder.toKey(k), encoder.encode(v))).toJSDictionary.asInstanceOf[js.Object]
  given Contravariant[ObjectEncoder] with
    def contramap[A, B](fa: ObjectEncoder[A])(f: B => A): ObjectEncoder[B] = (value: B) =>
      fa.toObject(f(value))

trait ObjectKeyEncoder[T]:
  def toKey(value: T): String

object ObjectKeyEncoder:
  def apply[T](using ke: ObjectKeyEncoder[T]): ObjectKeyEncoder[T] = ke
  def from[T](toKey: T => String): ObjectKeyEncoder[T] = value => toKey(value)

  given ObjectKeyEncoder[String] with
    def toKey(str: String): String = str

  given ObjectKeyEncoder[Int] = ObjectKeyEncoder[String].contramap(_.toString)
  given ObjectKeyEncoder[Float] = ObjectKeyEncoder[String].contramap(_.toString)
  given ObjectKeyEncoder[Double] = ObjectKeyEncoder[String].contramap(_.toString)

  given Contravariant[ObjectKeyEncoder] with
    def contramap[A, B](fa: ObjectKeyEncoder[A])(f: B => A): ObjectKeyEncoder[B] =
      value => fa.toKey(f(value))

trait ObjectKeyDecoder[T]:
  def fromString(key: String): T

object ObjectKeyDecoder:
  def apply[T](using kd: ObjectKeyDecoder[T]): ObjectKeyDecoder[T] = kd
  def from[T](fromString: String => T): ObjectKeyDecoder[T] = key => fromString(key)

  given ObjectKeyDecoder[String] with
    def fromString(key: String): String = key
  given ObjectKeyDecoder[Int] = ObjectKeyDecoder[String].map(_.toInt)
  given ObjectKeyDecoder[Float] = ObjectKeyDecoder[String].map(_.toFloat)
  given ObjectKeyDecoder[Double] = ObjectKeyDecoder[String].map(_.toDouble)

  given Functor[ObjectKeyDecoder] with
    def map[A, B](fa: ObjectKeyDecoder[A])(f: A => B): ObjectKeyDecoder[B] =
      key => f(fa.fromString(key))
