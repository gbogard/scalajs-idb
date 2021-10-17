package dev.guillaumebogard.idb.internal.future

import dev.guillaumebogard.idb.internal.Async
import dev.guillaumebogard.idb.internal.lowlevel.*
import dev.guillaumebogard.idb.api
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import scalajs.js
import js.JSConverters._

private[internal] object Database:
  def open[R](
      onUpgrade: UpgradeNeededEvent => R
  )(name: api.Database.Name, version: Int)(using ec: ExecutionContext): Future[IDBOpenResult[R]] =
    Future.fromTry(Try(indexedDB.open(name, version))).flatMap { openReq =>
      var upgradeResult: Option[R] = None
      Async[Future].async { cb =>
        openReq.onsuccess = event =>
          cb(
            Right(IDBOpenResult(openReq, event, event.target.result.asInstanceOf[IDBDatabase], upgradeResult))
          )
        openReq.onerror = _ => cb(Left(js.JavaScriptException(openReq.error.asInstanceOf[DOMException])))
        openReq.onupgradeneeded = event => {
          upgradeResult = Some(onUpgrade(event))
        }
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

  def addFuture(value: js.Any, key: Option[api.Key]): Future[Unit] =
    Future
      .fromTry(Try(store.add(value, key.orNull)))
      .flatMap(runRequest)
      .map(_ => ())

  def putFuture(value: js.Any, key: Option[api.Key]): Future[Unit] =
    Future
      .fromTry(Try(store.put(value, key.orNull)))
      .flatMap(runRequest)
      .map(_ => ())

private def runRequest[T, R](req: IDBRequest[T, R]): Future[IDBRequestResult[T, R]] =
  Async[Future].async { cb =>
    req.onsuccess = event => {
      val result = IDBRequestResult(req, event, req.result.asInstanceOf[R])
      cb(Right(result))
    }
    req.onerror = _ => {
      cb(Left(js.JavaScriptException(req.error.asInstanceOf[DOMException])))
    }
  }