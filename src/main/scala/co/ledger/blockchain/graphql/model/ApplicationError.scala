package co.ledger.blockchain.graphql.model

import io.circe.{Encoder, Json}

sealed trait ApplicationError extends Throwable {
  val code: Int
  val message: String
}

object ApplicationError {
  implicit def errorEncoder[T <: ApplicationError]: Encoder[T] = (e: T) => Json.obj(
    ("code", Json.fromInt(e.code)),
    ("message", Json.fromString(e.message))
  )
}
