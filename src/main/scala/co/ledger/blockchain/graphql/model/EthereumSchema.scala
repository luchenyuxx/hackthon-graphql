package co.ledger.blockchain.graphql.model

import cats.effect.IO
import cats.implicits._
import co.ledger.blockchain.graphql.model.EthereumModels._
import co.ledger.blockchain.graphql.module.LedgerExplorerETHClient
import co.ledger.blockchain.graphql.utils.crypto._
import co.ledger.blockchain.graphql.utils.future._
import io.circe.Json
import org.http4s.client.Client
import sangria.ast.Document
import sangria.execution._
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.marshalling.circe._
import sangria.schema.{Field, _}

import scala.concurrent.ExecutionContext.Implicits.global

object EthereumSchema {
  private implicit val hasIdBlock: HasId[Block, String] = (value: Block) => value.hash.hexString
  private implicit val hasHeightIdBlock: HasId[Block, Int] = (value: Block) => value.height
  private val blocksFetcher = Fetcher.caching(
    fetch = (ctx: LedgerExplorerETHClient, hashes: Seq[String]) =>
      hashes.toList.map(_.toHex.right.get).map(ctx.blockByHash).sequence.unsafeToFuture)
  private val blocksByHeightFetcher = Fetcher.caching(
    fetch = (ctx: LedgerExplorerETHClient, heights: Seq[Int]) =>
      heights.toList.map(ctx.blockByHeight).sequence.unsafeToFuture)
  private implicit val hasIdTransaction: HasId[Transaction, String] = (value: Transaction) => value.hash.hexString
  private val transactionsFetcher = Fetcher.caching(
    fetch = (ctx: LedgerExplorerETHClient, hashes: Seq[String]) =>
      hashes.toList.map(_.toHex.right.get).map(ctx.transactionByHash).sequence.unsafeToFuture)

  private val BlockType = ObjectType(
    "Block",
    "An ethereum block",
    fields[Unit, Block](
      Field("hash", StringType, resolve = _.value.hash.toString),
      Field("height", IntType, resolve = _.value.height),
      Field("time", StringType, resolve = _.value.time.toString),
      Field("transactions", ListType(StringType), resolve = _.value.txs.map(_.toString))
    )
  )

  private val BalanceType = ObjectType(
    "Balance",
    "Ethereum balance",
    fields[Unit, Balance](
      Field("address", StringType, resolve = _.value.address.toString),
      Field("balance", BigIntType, resolve = _.value.balance)
    )
  )
  private val TransferEventType = ObjectType(
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

  private val TransactionType = ObjectType(
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

  private val OptHashes = Argument("hashes", OptionInputType(ListInputType(StringType)))
  private val OptAddresses = Argument("address", OptionInputType(ListInputType(StringType)))
  private val OptHeights = Argument("heights", OptionInputType(ListInputType(IntType)))
  private val Hashes = Argument("hashes", ListInputType(StringType))
  private val Heights = Argument("heights", ListInputType(IntType))
  private val Addresses = Argument("addresses", ListInputType(StringType))
  private val TxHashes = Argument("txs", ListInputType(StringType))

  private val QueryType = ObjectType("Query",
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
      Field("transferEventsByTx", ListType(TransferEventType),
        description = Some("Return transfer events of a transaction"),
        arguments = TxHashes :: Nil,
        resolve = c => {
          DeferredValue(transactionsFetcher.deferSeq(c.arg(TxHashes))).map(_.flatMap(_.transferEvents))
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

  private val schema = Schema(QueryType)
  private val deferredResolver: DeferredResolver[LedgerExplorerETHClient] = DeferredResolver.fetchers(blocksFetcher, blocksByHeightFetcher, transactionsFetcher)
  def execute(query: Document, variables: Json)(client: Client[IO]): IO[Json] = Executor.execute(EthereumSchema.schema, query, new LedgerExplorerETHClient(client), deferredResolver = EthereumSchema.deferredResolver, variables = variables).toIO
}
