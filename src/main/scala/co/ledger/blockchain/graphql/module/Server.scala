package co.ledger.blockchain.graphql.module

import cats.effect._
import co.ledger.blockchain.graphql.controller.{GraphQLController, InternalController}
import co.ledger.blockchain.graphql.service.GraphQLService
import com.typesafe.scalalogging.StrictLogging
import fs2.Stream
import io.prometheus.client.CollectorRegistry
import org.http4s.client.Client
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

object Server extends StrictLogging {
  def initialize(config: Config, client: Client[IO])
                (implicit F: ConcurrentEffect[IO], timer: Timer[IO]): Stream[IO, ExitCode] = {

    lazy val collectorRegistry: CollectorRegistry = new CollectorRegistry
    lazy val graphQLService = new GraphQLService(client)
    lazy val internalController = new InternalController(collectorRegistry)
    lazy val graphQLController = new GraphQLController(graphQLService)

    val controllers = List(
      internalController,
      graphQLController
    )

    val router = Router(controllers.map(config.server.path -> _.routes): _*).orNotFound

    BlazeServerBuilder[IO]
      .bindHttp(config.server.port, config.server.host)
      .withHttpApp(router)
      .serve
  }
}
