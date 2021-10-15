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

import Backend.*
import cats.MonadError
import cats.implicits.given
import cats.data.NonEmptyList
import dev.guillaumebogard.idb.internal
import dev.guillaumebogard.idb.api
import java.util.Arrays
import scala.concurrent.*
import scala.scalajs.js
import scala.util.Try
import js.JSConverters._

trait Backend[F[_]]:
  given monadF: MonadError[F, Throwable]
  given asyncF: Async[F]

  private[internal] def delay[A](thunk: => A): F[A]

  /** Builds a [[IDBOpenDBRequest]] that can then be passed to [[runOpenRequest]] to open a new database
    */
  private[internal] def buildOpenRequest(name: api.Database.Name, version: Int): F[IDBOpenDBRequest] =
    delay(indexedDB.open(name, version))

  /** Opens a new transaction
    */
  private[internal] def buildTransaction(
      database: IDBDatabase,
      stores: NonEmptyList[api.ObjectStore.Name],
      mode: api.Transaction.Mode
  ): F[IDBTransaction] = delay(database.transaction(stores.toList.toJSArray, mode.toJS))

  private[internal] def runRequest[T, R](reqF: F[IDBRequest[T, R]]): F[IDBRequestResult[T, R]] =
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

  /** Runs a [[IDBOpenDBRequest]] and returns a [[FIDBOpenResult[R]]. When the 'upgradeneeded' event is fired
    * by the javascript runtime, the [[unsafeUpgrade]] callback is invoked synchromously
    */
  private[internal] def runOpenRequest[R](unsafeUpgrade: UpgradeNeededEvent => R)(
      reqF: F[IDBOpenDBRequest]
  ): F[IDBOpenResult[R]] =
    reqF.flatMap { req =>
      var upgradeResult: Option[R] = None
      Async[F].async { cb =>
        req.onsuccess = event =>
          cb(Right(IDBOpenResult(req, event, event.target.result.asInstanceOf[IDBDatabase], upgradeResult)))
        req.onerror = _ => cb(Left(js.JavaScriptException(req.error.asInstanceOf[DOMException])))
        req.onupgradeneeded = event => {
          upgradeResult = Some(unsafeUpgrade(event))
        }
      }
    }

object Backend:
  def apply[F[_]](using b: Backend[F]): Backend[F] = b

  def openDatabase[F[_]](name: api.Database.Name, schema: api.Schema)(using
      backend: Backend[F]
  ): F[Either[Throwable, api.Database[F]]] = {
    import backend.given



    backend
      .runOpenRequest(SchemaExecutor.unsafeMigrateSchema(_, schema))(
        backend.buildOpenRequest(name, schema.lastVersion)
      )
      .map(res => buildDatabaseFromJS(res.database))
      .attempt
  }

  given (using ec: ExecutionContext): Backend[Future] with
    val monadF = MonadError[Future, Throwable]
    val asyncF = Async[Future]
    def delay[A](thunk: => A): Future[A] = Future.fromTry(Try(thunk))

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

  private def buildDatabaseFromJS[F[_]: Backend](db: IDBDatabase) =
    new api.Database[F]:
      def transact[T](
        mode: api.Transaction.Mode, 
        stores: NonEmptyList[api.ObjectStore.Name]
      )(transaction: api.Transaction[T]) =
        TransactionInterpreter(db, stores, mode, transaction)
