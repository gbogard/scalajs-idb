package dev.guillaumebogard.idb.api

type Backend[F[_]] = dev.guillaumebogard.idb.internal.Backend[F]

object Backend:
  def apply[F[_]](using b: Backend[F]): Backend[F] = b

