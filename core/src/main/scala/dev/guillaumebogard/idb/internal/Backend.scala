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

import cats.data.NonEmptyList
import dev.guillaumebogard.idb.api
import dev.guillaumebogard.idb.internal.future.*
import scala.concurrent.*
import scala.util.Try

trait Backend[F[_]]:
  private[internal] def delay[A](thunk: => A): F[A]
  private[internal] def fromFuture[A](fa: F[Future[A]]): F[A]
  private[internal] given executionContext: ExecutionContext

object Backend:
  def apply[F[_]](using b: Backend[F]): Backend[F] = b

  def openDatabase[F[_]](name: api.Database.Name, schema: api.Schema)(using
      backend: Backend[F]
  ): F[api.Database[F]] =
    backend.fromFuture(backend.delay {
      import backend.given
      Database
        .open(SchemaExecutor.unsafeMigrateSchema(_, schema))(name, schema.lastVersion)
        .map(_.database)
        .map(db =>
          new api.Database[F]:
            def transact[T](mode: api.Transaction.Mode, stores: NonEmptyList[api.ObjectStore.Name])(
                transaction: api.Transaction[T]
            ): F[T] =
              backend.fromFuture(backend.delay(db.transactFuture(stores, mode)(transaction)))
        )
    })

  given (using ec: ExecutionContext): Backend[Future] with
    def delay[A](thunk: => A): Future[A] = Future.fromTry(Try(thunk))
    def fromFuture[A](fa: Future[Future[A]]): Future[A] = fa.flatten
    val executionContext = ec
