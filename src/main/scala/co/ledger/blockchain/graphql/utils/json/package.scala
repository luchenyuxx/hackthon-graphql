package co.ledger.blockchain.graphql.utils

import java.time.Instant

import cats.effect.IO
import cats.syntax.either._
import co.ledger.blockchain.graphql.utils.crypto.Hex
import io.circe.{Decoder, Encoder}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

package object json {
  implicit val instantDecoder: Decoder[Instant] = Decoder.decodeString.emap { input =>
    Either.catchNonFatal(Instant.parse(input)).leftMap(_ => "Instant")
  }

  implicit val instantEncoder: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

  implicit val hexDecoder: Decoder[Hex] = Decoder.decodeString.emap(Hex.apply)

  implicit val hexEncoder: Encoder[Hex] = Encoder.encodeString.contramap[Hex](_.toString)

  implicit def mapEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[IO, Map[String, A]] = jsonEncoderOf[IO, Map[String, A]]
}
