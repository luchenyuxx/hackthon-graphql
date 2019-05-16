package co.ledger.blockchain.graphql.service

import cats.effect.IO
import org.http4s.client.Client

class GraphQLService(client: Client[IO]) {
  // avoid fetal warning
  def test() = println(client)
}
