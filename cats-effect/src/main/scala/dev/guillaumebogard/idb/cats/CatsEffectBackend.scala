package dev.guillaumebogard.idb.cats

import cats.effect.Async
import dev.guillaumebogard.idb.api.*
import dev.guillaumebogard.idb.internal

given [F[_]](using catsEffectAsync: Async[F]): internal.Async[F] with
  def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] =
    catsEffectAsync.async_(k)

given [F[_]](using async: Async[F]): Backend[F] with
  val asyncF = internal.Async[F]
  val monadF = async
  def delay[A](thunk: => A): F[A] = async.delay(thunk)
