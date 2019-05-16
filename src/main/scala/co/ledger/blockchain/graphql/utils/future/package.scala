package co.ledger.blockchain.graphql.utils

import java.util.concurrent.{CompletableFuture, CompletionException}

import cats.effect.IO

import scala.concurrent.CancellationException

package object future {
  def fromJavaFuture[A](cf: => CompletableFuture[A]): IO[A] = {
    IO.cancelable(cb => {
      val cfx = cf.handle[Unit]((result: A, err: Throwable) => {
        err match {
          case null => cb(Right(result))
          case _: CancellationException => ()
          case ex: CompletionException if ex.getCause ne null => cb(Left(ex.getCause))
          case ex => cb(Left(ex))
        }
      })
      IO(cfx.cancel(true))
    })
  }
}
