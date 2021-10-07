  package dev.guillaumebogard.idb.api

import scala.scalajs.js
import js.JSConverters._

enum KeyPath:
  case Empty
  case Identifier(id: String)
  case Path(ids: Seq[Identifier])

object KeyPath:
  opaque type JS = js.Array[String] | Null

  given Conversion[KeyPath, JS] = toJS(_)

  def fromJS(input: JS): KeyPath = input match
    case arr: js.Array[String] => Path(arr.map(Identifier(_): Identifier).toSeq)
    case null => Empty

  extension (keyPath: KeyPath)
    def toJS: KeyPath.JS = keyPath match
      case Empty => null
      case Identifier(id) => js.Array(id)
      case Path(ids) => ids.toJSArray.map(_.id)
