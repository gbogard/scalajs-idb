package dev.guillaumebogard.idb

import _root_.cats.effect.IO
import _root_.cats.*
import _root_.cats.effect.unsafe.implicits.global
import dev.guillaumebogard.idb.cats.given

object IOTests extends TestsUsingBackend[IO]([A] => (fa: IO[A]) => fa.unsafeToFuture())
