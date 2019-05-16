package co.ledger.blockchain.graphql.controller

import cats.effect.IO
import cats.implicits._
import co.ledger.blockchain.graphql.model.EthSchema._
import co.ledger.blockchain.graphql.module.LedgerExplorerETHClient
import co.ledger.blockchain.graphql.utils.crypto._
import co.ledger.blockchain.graphql.utils.future._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Field, _}

import scala.concurrent.ExecutionContext.Implicits.global

class GraphQLController(client: Client[IO]) extends HttpController {

  case class Query(query: String)

  implicit val queryDecoder: Decoder[Query] = deriveDecoder

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req@GET -> Root / "graphql" =>
      for {
        gq <- req.as[Query]
        query <- QueryParser.parse(gq.query).toEither.liftTo[IO]
        result <- Executor.execute(EthereumQuery.schema, query, EthereumQuery.context(client)).toIO
        r <- Ok(result)
      } yield r
  }

}

object EthereumQuery {

  val BlockType = ObjectType(
    "Block",
    "An ethereum block",
    fields[Unit, Block](
      Field("hash", StringType, resolve = _.value.hash.toString),
      Field("height", IntType, resolve = _.value.height),
      Field("time", StringType, resolve = _.value.time.toString),
      Field("transactions", ListType(StringType), resolve = _.value.txs.map(_.toString))
    )
  )
  val BalanceType = ObjectType(
    "Balance",
    "Ethereum balance",
    fields[Unit, Balance](
      Field("address", StringType, resolve = _.value.address.toString),
      Field("balance", BigIntType, resolve = _.value.balance)
    )
  )
  val TransferEventType = ObjectType(
    "TransferEvent",
    "Ethereum transfer event",
    fields[Unit, TransferEvent](
      Field("contract", StringType, resolve = _.value.contract.toString),
      Field("from", StringType, resolve = _.value.from.toString),
      Field("to", StringType, resolve = _.value.to.toString),
      Field("count", BigIntType, resolve = _.value.count),
      Field("decimal", OptionType(IntType), resolve = _.value.decimal),
      Field("symbol", OptionType(StringType), resolve = _.value.symbol)
    )
  )

  val TransactionType = ObjectType(
    "Transaction",
    "An Ethereum transaction",
    fields[Unit, Transaction](
      Field("hash", StringType, resolve = _.value.hash.toString),
      Field("status", IntType, resolve = _.value.status),
      Field("receivedAt", StringType, resolve = _.value.receivedAt.toString),
      Field("value", BigIntType, resolve = _.value.value),
      Field("gas", BigIntType, resolve = _.value.gas),
      Field("gasPrice", BigIntType, resolve = _.value.gasPrice),
      Field("from", StringType, resolve = _.value.from.toString),
      Field("to", StringType, resolve = _.value.from.toString),
      Field("input", StringType, resolve = _.value.input.toString),
      Field("cumulativeGasUsed", BigIntType, resolve = _.value.cumulativeGasUsed),
      Field("gasUsed", BigIntType, resolve = _.value.gasUsed),
      Field("transferEvents", ListType(TransferEventType), resolve = _.value.transferEvents),
      Field("block", BlockType, resolve = _.value.block)
    )
  )

  val OptHashes = Argument("hashes", OptionInputType(ListInputType(StringType)))
  val Hashes = Argument("hashes", ListInputType(StringType))
  val Heights = Argument("heights", OptionInputType(ListInputType(IntType)))
  val OptAddresses = Argument("address", OptionInputType(ListInputType(StringType)))
  val Addresses = Argument("address", ListInputType(StringType))

  val QueryType = ObjectType("Query",
    fields[LedgerExplorerETHClient, Unit](
      Field("block", ListType(BlockType),
        description = Some("Returns a block with specific `hash`."),
        arguments = OptHashes :: Heights :: Nil,
        resolve = c => {
          val hashes = c.arg(OptHashes).map(_.map(_.toHex.right.get)).getOrElse(List())
          val heights = c.arg(Heights).getOrElse(List())
          val blocks = hashes.map(c.ctx.blockByHash) ++
            heights.map(c.ctx.blockByHeight)
          blocks.toList.sequence.flatMap { bs =>
            if (bs.isEmpty) c.ctx.currentBlock.map(List(_))
            else IO.pure(bs)
          }.unsafeToFuture
        }),
      Field("transaction", ListType(TransactionType),
        description = Some("return transaction by hash"),
        arguments = OptHashes :: OptAddresses :: Nil,
        resolve = c => {
          val hashes = c.arg(OptHashes).map(_.map(_.toHex.right.get)).getOrElse(List())
          val addresses = c.arg(OptAddresses).map(_.map(_.toHex.right.get)).getOrElse(List())
          val txOfAddresses = addresses.map(c.ctx.transactionsByAddress).toList.sequence.map(_.flatten)
          val txOfHashes = hashes.map(c.ctx.transactionByHash).toList.sequence
          (txOfHashes, txOfAddresses).mapN(_ ++ _).unsafeToFuture
        }),
      Field("balance", ListType(BalanceType),
        arguments = Addresses :: Nil,
        resolve = c => {
          val hashes = c.arg(Addresses).map(_.toHex.right.get)
          hashes.map(c.ctx.addressBalance).toList.sequence.unsafeToFuture
        }
      )
    )
  )

  val schema = Schema(QueryType)

  def context(client: Client[IO]): LedgerExplorerETHClient = new LedgerExplorerETHClient(client)
}

