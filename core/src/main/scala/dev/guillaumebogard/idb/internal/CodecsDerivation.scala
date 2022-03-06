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

package dev.guillaumebogard.idb.internal

import scala.deriving.*
import scala.compiletime.*
import scala.scalajs.js
import dev.guillaumebogard.idb.api.*
import Derivation.*

trait EncoderDerivation:
  inline final def derived[T](using inline T: Mirror.Of[T]): ObjectEncoder[T] =
    (value: T) =>
      lazy val labels = summonLabels[T.MirroredElemLabels]
      lazy val encoders = summonEncoders[T.MirroredElemTypes]
      inline T match
        case m: Mirror.ProductOf[T] =>
          val product: Product = value.asInstanceOf
          val pairs: Iterator[(String, js.Any)] = product.productIterator.zipWithIndex.map({
            case (value, ix) =>
              val label = labels(ix)
              val encoder = encoders(ix)
              (label, encoder.encode(value.asInstanceOf))
          })
          js.Dictionary[js.Any](pairs.toSeq*).asInstanceOf[js.Object]
        case m: Mirror.SumOf[T] =>
          val ix = m.ordinal(value)
          val label = labels(ix)
          val encoder = encoders(ix)
          js.Dictionary[js.Any](label -> encoder.encode(value.asInstanceOf)).asInstanceOf[js.Object]

trait DecoderDerivation:
  inline final def derived[T](using inline T: Mirror.Of[T]): Decoder[T] =
    (jsValue: js.Any) =>
      lazy val labels = summonLabels[T.MirroredElemLabels]
      lazy val decoders = summonDecoders[T.MirroredElemTypes]
      lazy val dict = jsValue.asInstanceOf[js.Dictionary[js.Any]]
      inline T match
        case m: Mirror.ProductOf[T] =>
          val decodedValues: Array[Any] = labels.zipWithIndex.map({ case (label, ix) =>
            val decoder = decoders(ix)
            val value = dict.get(label).getOrElse(null.asInstanceOf[js.Any])
            decoder.decode(value)
          })
          m.fromProduct(Tuple.fromArray(decodedValues))
        case m: Mirror.SumOf[T] =>
          dict.iterator
            .map({ case (dictKey, value) =>
              labels.lastIndexOf(dictKey) match
                case -1 => Option.empty[T]
                case ix =>
                  val decoder = decoders(ix)
                  Some(decoder.decode(value.asInstanceOf).asInstanceOf[T])
            })
            .collectFirst({ case Some(res) => res })
            .get

private[internal] object Derivation:
  inline final def summonLabels[T <: Tuple]: Array[String] = summonLabelsRec[T].toArray
  inline final def summonDecoders[T <: Tuple]: Array[Decoder[?]] = summonDecodersRec[T].toArray
  inline final def summonEncoders[T <: Tuple]: Array[Encoder[?]] = summonEncodersRec[T].toArray

  inline final def summonEncoder[A]: Encoder[A] = summonFrom {
    case encodeA: Encoder[A] => encodeA
    case _: Mirror.Of[A]     => ObjectEncoder.derived[A]
  }

  inline final def summonDecoder[A]: Decoder[A] = summonFrom {
    case decodeA: Decoder[A] => decodeA
    case _: Mirror.Of[A]     => Decoder.derived[A]
  }

  inline final def summonLabelsRec[T <: Tuple]: List[String] = inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => constValue[t].asInstanceOf[String] :: summonLabelsRec[ts]

  inline final def summonDecodersRec[T <: Tuple]: List[Decoder[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonDecoder[t] :: summonDecodersRec[ts]

  inline final def summonEncodersRec[T <: Tuple]: List[Encoder[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonEncoder[t] :: summonEncodersRec[ts]
