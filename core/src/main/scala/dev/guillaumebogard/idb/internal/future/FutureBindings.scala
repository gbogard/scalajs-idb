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

package dev.guillaumebogard.idb.internal.future

import dev.guillaumebogard.idb.internal.Async
import dev.guillaumebogard.idb.internal.lowlevel.*
import dev.guillaumebogard.idb.api
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import scalajs.js
import js.JSConverters.*

private[internal] object Database:
  def open[R](
      onUpgrade: UpgradeNeededEvent => R
  )(name: api.Database.Name, version: Int)(using ec: ExecutionContext): Future[IDBOpenResult[R]] =
    Future.fromTry(Try(indexedDB.open(name, version))).flatMap { openReq =>
      var upgradeResult: Option[R] = None
      Async[Future].async { cb =>
        openReq.onsuccess = event =>
          cb(
            Right(
              IDBOpenResult(
                openReq,
                event,
                event.target.result.asInstanceOf[IDBDatabase],
                upgradeResult
              )
            )
          )
        openReq.onerror =
          _ => cb(Left(js.JavaScriptException(openReq.error.asInstanceOf[DOMException])))
        openReq.onupgradeneeded = event => upgradeResult = Some(onUpgrade(event))
      }
    }

private[internal] final case class IDBRequestResult[Target, Result](
    req: IDBRequest[Target, Result],
    event: DOMEvent[Target],
    result: Result
)

private[internal] final case class IDBOpenResult[UpgradeResult](
    req: IDBOpenDBRequest,
    event: DOMEvent[IDBOpenDBRequest],
    database: IDBDatabase,
    upgradeResult: Option[UpgradeResult]
)

extension (db: IDBDatabase)
  private[internal] def createOjectStoreFuture(
      name: api.ObjectStore.Name,
      options: IDBObjectStore.CreateObjectStoreOptions
  ): Future[IDBObjectStore] = Future.fromTry(Try(db.createObjectStore(name, options)))

  private[internal] def transactionFuture(
      stores: Seq[api.ObjectStore.Name],
      mode: api.Transaction.Mode
  ): Future[IDBTransaction] = Future.fromTry(Try(db.transaction(stores.toJSArray, mode)))

extension (transaction: IDBTransaction)
  private[internal] def objectStoreFuture(name: api.ObjectStore.Name): Future[IDBObjectStore] =
    Future.fromTry(Try(transaction.objectStore(name)))

extension (store: IDBObjectStore)(using ec: ExecutionContext)
  private[internal] def getFuture(key: api.Key): Future[Option[js.Any]] =
    Future
      .fromTry(Try(store.get(key)))
      .flatMap(runRequest)
      .map(_.result.toOption)

  private[internal] def deleteFuture(keyOrKeyRange: api.Key | api.KeyRange): Future[Unit] =
    Future
      .fromTry(Try(store.delete(keyOrKeyRange)))
      .flatMap(runRequest)
      .map(_.result)

  private[internal] def getAllFuture(range: Option[api.KeyRange], count: Int): Future[Seq[js.Any]] =
    Future
      .fromTry(Try(store.getAll(range.orNull, count)))
      .flatMap(runRequest)
      .map(_.result.toSeq)

  private[internal] def addFuture(value: js.Any, key: Option[api.Key]): Future[api.Key] =
    Future
      .fromTry(Try(store.add(value, key.orUndefined)))
      .flatMap(runRequest)
      .map(_.result)

  private[internal] def putFuture(value: js.Any, key: Option[api.Key]): Future[api.Key] =
    Future
      .fromTry(Try(store.put(value, key.orUndefined)))
      .flatMap(runRequest)
      .map(_.result)

private def runRequest[T, R](req: IDBRequest[T, R]): Future[IDBRequestResult[T, R]] =
  Async[Future].async { cb =>
    req.onsuccess = event =>
      val result = IDBRequestResult(req, event, req.result.asInstanceOf[R])
      cb(Right(result))
    req.onerror = _ => cb(Left(js.JavaScriptException(req.error.asInstanceOf[DOMException])))
  }

  given ec: ExecutionContext = ???
  val program: Future[String] =
    for
      dbRes <- Database.open(upgrade)(api.Database.Name("test"), 1)
      db = dbRes.database
      usersStoreName = api.ObjectStore.Name("users")
      transaction <- db.transactionFuture(Seq(usersStoreName), api.Transaction.Mode.ReadWrite)
      usersStore <- transaction.objectStoreFuture(usersStoreName)
      _ <- usersStore.putFuture("Paul", api.toKey(1).some)
      user <- usersStore.getFuture(api.toKey(1))
    yield user.asInstanceOf
