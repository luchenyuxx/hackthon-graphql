package co.ledger.blockchain.graphql.utils

import cats.syntax.either._
import cats.{ApplicativeError, MonadError}

import scala.util.Try

package object throwable {

  implicit class ThrowableOps[A](from: Throwable) {
    def raise[F[_]](implicit F: ApplicativeError[F, Throwable]): F[A] = {
      ApplicativeError[F, Throwable].raiseError[A](from)
    }

    def raise[F[_], B](implicit F: ApplicativeError[F, Throwable], s: DummyImplicit): F[B] = {
      ApplicativeError[F, Throwable].raiseError[B](from)
    }
  }

  implicit class MonadErrorOps[F[_], E <: Throwable, A](fa: F[A])(implicit F: MonadError[F, E]) {
    def mapError(f: String => E): F[A] = F.adaptError(fa) { case e => f(e.getMessage) }
  }

  def safely[A](cb: => A): Either[String, A] = {
    Try(cb).toEither.leftMap(_.toString)
  }
}
