package dev.guillaumebogard.idb.internal

import cats.Monad.ops.*
import dev.guillaumebogard.idb.Backend
import dev.guillaumebogard.idb.Backend.given
import dev.guillaumebogard.idb.api
import dev.guillaumebogard.idb.internal.*
import scala.concurrent.{ExecutionContext, Future}
import scalajs.js

def runRequest[F[_]: Backend, T, R](reqF: F[IDBRequest[T, R]]): F[IDBRequestResult[T, R]] =
  reqF.flatMap { req =>
    Async[F].async { cb =>
      req.onsuccess = event => {
        val result = IDBRequestResult(req, event, req.result.asInstanceOf[R])
        cb(Right(result))
      }
      req.onerror = _ => {
        cb(Left(js.JavaScriptException(req.error.asInstanceOf[DOMException])))
      }
    }
  }

def runOpenRequest[F[_]: Backend, R](upgrade: UpgradeNeededEvent => R)(
    reqF: F[IDBOpenDBRequest]
): F[IDBOpenResult[R]] =
  reqF.flatMap { req =>
    var upgradeResult: Option[R] = None
    Async[F].async { cb =>
      req.onsuccess = event =>
        cb(Right(IDBOpenResult(req, event, event.target.result.asInstanceOf[IDBDatabase], upgradeResult)))
      req.onerror = _ => cb(Left(js.JavaScriptException(req.error.asInstanceOf[DOMException])))
      req.onupgradeneeded = event => {
        upgradeResult = Some(upgrade(event))
      }
    }
  }

final case class IDBRequestResult[Target, Result](
    req: IDBRequest[Target, Result],
    event: DOMEvent[Target],
    result: Result
)

final case class IDBOpenResult[UpgradeResult](
    req: IDBOpenDBRequest,
    event: DOMEvent[IDBOpenDBRequest],
    database: IDBDatabase,
    upgradeResult: Option[UpgradeResult]
)
