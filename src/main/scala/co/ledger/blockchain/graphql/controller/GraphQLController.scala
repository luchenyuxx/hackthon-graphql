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
import sangria.execution.deferred.{DeferredResolver, Fetcher, FetcherCache, FetcherConfig, HasId}
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
        result <- Executor.execute(EthereumQuery.schema, query, EthereumQuery.context(client), deferredResolver = EthereumQuery.deferredResolver).toIO
        r <- Ok(result)
      } yield r
  }

}

object EthereumQuery {
  val cache = FetcherCache.simple

  implicit val hasIdBlock: HasId[Block, String] = (value: Block) => value.hash.hexString
  implicit val hasHeightIdBlock: HasId[Block, Int] = (value: Block) => value.height
  val blocksFetcher = Fetcher(
    config = FetcherConfig.caching(cache),
    fetch = (ctx: LedgerExplorerETHClient, hashes: Seq[String]) =>
      hashes.toList.map(_.toHex.right.get).map(ctx.blockByHash).sequence.unsafeToFuture)
  val blocksByHeightFetcher = Fetcher(
    config = FetcherConfig.caching(cache),
    fetch = (ctx: LedgerExplorerETHClient, heights: Seq[Int]) =>
      heights.toList.map(ctx.blockByHeight).sequence.unsafeToFuture)
  implicit val hasIdTransaction: HasId[Transaction, String] = (value: Transaction) => value.hash.hexString
  val transactionsFetcher = Fetcher(
    config = FetcherConfig.caching(cache),
    fetch = (ctx: LedgerExplorerETHClient, hashes: Seq[String]) =>
      hashes.toList.map(_.toHex.right.get).map(ctx.transactionByHash).sequence.unsafeToFuture)
  val transactionsByAddressFetcer = Fetcher(
    config = FetcherConfig.caching(cache),
    fetch = (ctx: LedgerExplorerETHClient, addresses: Seq[String]) =>
      addresses.toList.map(_.toHex.right.get).map(ctx.transactionsByAddress).sequence.map(_.flatten).unsafeToFuture)

  val deferredResolver = DeferredResolver.fetchers(blocksFetcher, blocksByHeightFetcher, transactionsByAddressFetcer, transactionsFetcher)

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
  val OptAddresses = Argument("address", OptionInputType(ListInputType(StringType)))
  val OptHeights = Argument("heights", OptionInputType(ListInputType(IntType)))
  val Hashes = Argument("hashes", ListInputType(StringType))
  val Heights = Argument("heights", ListInputType(IntType))
  val Addresses = Argument("addresses", ListInputType(StringType))
  val TxHashes = Argument("txs", ListInputType(StringType))

  val QueryType = ObjectType("Query",
    fields[LedgerExplorerETHClient, Unit](
      Field("blocks", ListType(BlockType),
        description = Some("Returns a block with specific `hash`."),
        arguments = OptHashes :: OptHeights :: Nil,
        resolve = c => {
          val hashes = c.arg(OptHashes).map(_.map(_.toHex.right.get)).getOrElse(List())
          val heights = c.arg(OptHeights).getOrElse(List())
          val blocks = hashes.map(c.ctx.blockByHash) ++
            heights.map(c.ctx.blockByHeight)
          blocks.toList.sequence.flatMap { bs =>
            if (bs.isEmpty) c.ctx.currentBlock.map(List(_))
            else IO.pure(bs)
          }.unsafeToFuture
        }),
      Field("transactions", ListType(TransactionType),
        description = Some("return transaction by hash"),
        arguments = OptHashes :: OptAddresses :: Nil,
        resolve = c => {
          val hashes = c.arg(OptHashes).map(_.map(_.toHex.right.get)).getOrElse(List())
          val addresses = c.arg(OptAddresses).map(_.map(_.toHex.right.get)).getOrElse(List())
          val txOfAddresses = addresses.map(c.ctx.transactionsByAddress).toList.sequence.map(_.flatten)
          val txOfHashes = hashes.map(c.ctx.transactionByHash).toList.sequence
          (txOfHashes, txOfAddresses).mapN(_ ++ _).unsafeToFuture
        }),
      Field("currentBlock", BlockType,
        description = Some("Return top block on the blockchain"),
        resolve = c => {
          c.ctx.currentBlock.unsafeToFuture
        }),
      Field("blocksByHash", ListType(BlockType),
        description = Some("Return blocks with input hashes"),
        arguments = Hashes :: Nil,
        resolve = c => {
          blocksFetcher.deferSeq(c.arg(Hashes))
        }),
      Field("blocksByHeight", ListType(BlockType),
        description = Some("Return blocks with input heights"),
        arguments = Heights :: Nil,
        resolve = c => {
          blocksByHeightFetcher.deferSeq(c.arg(Heights))
        }),
      Field("transactionsByHash", ListType(TransactionType),
        description = Some("Return transactions by hash"),
        arguments = Hashes :: Nil,
        resolve = c => {
          transactionsFetcher.deferSeq(c.arg(Hashes))
        }),
      Field("transactionsByAddress", ListType(TransactionType),
        description = Some("Return transactions by address"),
        arguments = Addresses :: Nil,
        resolve = c => {
          transactionsByAddressFetcer.deferSeq(c.arg(Addresses))
        }),
      Field("transferEventsByTx", ListType(TransferEventType),
        description = Some("Return transfer events of a transaction"),
        arguments = TxHashes :: Nil,
        resolve = c => {
          DeferredValue(transactionsFetcher.deferSeq(c.arg(TxHashes))).map(_.flatMap(_.transferEvents))
        }),
      Field("transferEventsByAddress", ListType(TransferEventType),
        description = Some("Return transfer events of an address"),
        arguments = Addresses :: Nil,
        resolve = c => {
          DeferredValue(transactionsByAddressFetcer.deferSeq(c.arg(Addresses))).map(_.flatMap(_.transferEvents))
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

