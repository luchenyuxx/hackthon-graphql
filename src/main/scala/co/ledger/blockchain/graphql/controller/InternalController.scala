package co.ledger.blockchain.graphql.controller

import buildinfo.BuildInfo
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import io.prometheus.client.CollectorRegistry
import org.http4s.HttpRoutes
import org.http4s.metrics.prometheus.PrometheusExportService
import org.http4s.circe.CirceEntityEncoder._

class InternalController(registry: CollectorRegistry) extends HttpController with StrictLogging {
  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "_health" => Ok(Map(
      "status" -> "OK"
    ))

    case GET -> Root / "_version" => Ok(Map(
      "version" -> BuildInfo.version,
      "sha1" -> BuildInfo.gitHeadCommit.getOrElse("n/a")
    ))

    case GET -> Root / "_metrics" =>
      PrometheusExportService.generateResponse[IO](registry)
  }
}
