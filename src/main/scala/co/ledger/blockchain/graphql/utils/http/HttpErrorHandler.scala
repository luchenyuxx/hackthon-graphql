package co.ledger.blockchain.graphql.utils.http

import cats.ApplicativeError
import cats.data.{Kleisli, OptionT}
import cats.syntax.applicativeError._
import cats.syntax.functor._
import org.http4s.{HttpRoutes, Request, Response}

abstract class HttpErrorHandler[F[_], E <: Throwable](implicit ev: ApplicativeError[F, E]) {
  def handler: E => F[Response[F]]

  def <<(routes: HttpRoutes[F]): HttpRoutes[F] = handle(routes)

  def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes.run(req).value.handleErrorWith { e => handler(e).map(Option(_)) }
      }
    }
}

object HttpErrorHandler {
  def apply[F[_], E <: Throwable](_handler: E => F[Response[F]])(implicit ev: ApplicativeError[F, E]): HttpErrorHandler[F, E] = new HttpErrorHandler {
    override def handler: E => F[Response[F]] = _handler
  }
}
