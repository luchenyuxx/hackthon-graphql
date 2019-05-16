package co.ledger.blockchain.graphql.controller

import cats.effect.IO
import io.circe.Printer
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

trait HttpController extends Http4sDsl[IO] {
  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  def routes: HttpRoutes[IO]
}
