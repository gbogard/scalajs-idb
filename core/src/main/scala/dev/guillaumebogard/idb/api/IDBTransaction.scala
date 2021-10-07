package dev.guillaumebogard.idb.api

trait IDBTransaction[F[_]]

object IDBTransaction:
  enum Mode:
    case ReadWrite, ReadOnly

  object Mode:
    opaque type JS = String
    extension (mode: Mode)
      def toJS: JS =
        mode match
          case ReadOnly  => "readonly"
          case ReadWrite => "readwrite"
    given Conversion[Mode, JS] = toJS(_)

