package co.ledger.blockchain.graphql.utils

import java.time.Instant

import cats.syntax.either._
import co.ledger.blockchain.graphql.utils.throwable.safely
import org.http4s.{ParseFailure, QueryParamDecoder, QueryParameterValue}

package object http {
  implicit val instantQueryParamDecoder: QueryParamDecoder[Instant] = (value: QueryParameterValue) => {
    safely(Instant.parse(value.value)).leftMap(ParseFailure("Failed to parse instant", _)).toValidatedNel
  }
}
