package co.ledger.blockchain.graphql.utils

import java.time.Instant
import java.time.format.DateTimeFormatter

import cats.effect.IO

package object date {
  def parseDate(dateFormat: DateTimeFormatter, input: String): IO[Instant] = IO {
    Instant.from(dateFormat.parse(input))
  }
}
