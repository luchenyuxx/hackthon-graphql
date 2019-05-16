package co.ledger.blockchain.graphql.module

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig.generic.auto._

case class Config(server: ServerConfig)

case class ServerConfig(host: String, port: Int, path: String)

object Config {
  def load: IO[Config] = IO {
    pureconfig.loadConfigOrThrow[Config](ConfigFactory.load)
  }
}
