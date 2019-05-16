package co.ledger.blockchain.graphql

import cats.effect.{ExitCode, IO, IOApp, Resource}
import co.ledger.blockchain.graphql.module.{Config, Server}
import com.typesafe.scalalogging.StrictLogging
import org.http4s.client.asynchttpclient.AsyncHttpClient

object App extends IOApp with StrictLogging {
  def run(args: List[String]): IO[ExitCode] = {
    val resources = for {
      config <- Resource.liftF(Config.load)
      client <- AsyncHttpClient.resource[IO]()
    } yield (config, client)

    resources.use { case (config, client) =>
      Server.initialize(config, client).compile.lastOrError
    }
  }
}
