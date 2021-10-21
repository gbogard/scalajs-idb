package dev.guillaumebogard.idb.time

import java.time.*
import dev.guillaumebogard.idb.api.*
import cats.implicits.*

given Encoder[Instant] = Encoder[String].contramap(_.toString)
given Decoder[Instant] = Decoder[String].map(Instant.parse(_).nn)

given Encoder[LocalDate] = Encoder[String].contramap(_.toString)
given Decoder[LocalDate] = Decoder[String].map(LocalDate.parse(_).nn)
