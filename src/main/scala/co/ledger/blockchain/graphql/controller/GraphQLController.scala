package co.ledger.blockchain.graphql.controller

import cats.effect.IO
import cats.implicits._
import co.ledger.blockchain.graphql.model.EthereumSchema
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import sangria.parser.QueryParser

class GraphQLController(client: Client[IO]) extends HttpController {

  case class Query(query: String)

  implicit val queryDecoder: Decoder[Query] = deriveDecoder

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@GET -> Root / "graphql" =>
      for {
        gq <- req.as[Query]
        query <- QueryParser.parse(gq.query).toEither.liftTo[IO]
        result <- EthereumSchema.execute(query)(client)
        r <- Ok(result)
      } yield r
  }

}

