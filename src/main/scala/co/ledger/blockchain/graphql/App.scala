package co.ledger.blockchain.graphql

import cats.effect.{ExitCode, IO, IOApp, Resource}
import co.ledger.blockchain.graphql.module.{Config, Server}
import com.typesafe.scalalogging.StrictLogging

object App extends IOApp with StrictLogging {
  def run(args: List[String]): IO[ExitCode] = {
    val resources = for {
      config <- Resource.liftF(Config.load)
    } yield (config)

    resources.use { case (config) =>
      Server.initialize(config).compile.lastOrError
    }
  }
}
