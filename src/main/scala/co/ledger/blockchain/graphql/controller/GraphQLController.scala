package co.ledger.blockchain.graphql.controller
import cats.effect.IO
import co.ledger.blockchain.graphql.service.GraphQLService
import org.http4s.HttpRoutes

class GraphQLController(graphQLService: GraphQLService) extends HttpController {
  override def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case POST -> Root / "graphql" =>
      // to avoid fetal warning
      println(graphQLService)
      Ok("building")
  }
}
