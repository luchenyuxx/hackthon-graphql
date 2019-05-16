package co.ledger.blockchain.graphql.model

import io.circe.{Encoder, Json}

sealed trait ApplicationError extends Throwable {
  val code: Int
  val message: String
}

case class TransactionError(cause: String) extends ApplicationError {
  val code = 1
  val message = s"Failed to create transaction: $cause"
}

case class TransactionBroadcastError(cause: String) extends ApplicationError {
  val code = 2
  val message = s"Failed to broadcast transaction: $cause"
}

case class EntryCreationError(cause: String) extends ApplicationError {
  val code = 3
  val message = s"Failed to create entry: $cause"
}

object ApplicationError {
  implicit def errorEncoder[T <: ApplicationError]: Encoder[T] = (e: T) => Json.obj(
    ("code", Json.fromInt(e.code)),
    ("message", Json.fromString(e.message))
  )
}
