package co.ledger.blockchain.graphql.module

import cats.effect.IO
import co.ledger.blockchain.graphql.model.EthSchema.{Balance, Block, Transaction, TransferEvent}
import co.ledger.blockchain.graphql.utils.crypto.Hex
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client

class LedgerExplorerETHClient(client: Client[IO]) {
  private val baseUrl = uri"http://eth-mainnet.explorers.dev.aws.ledger.fr/blockchain/v3/"
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
  private implicit val blockDecoder: Decoder[Block] = deriveDecoder
  private implicit val blockEncoder: Encoder[Block] = deriveEncoder
  private implicit val balanceDecoder: Decoder[Balance] = deriveDecoder
  implicit val teDecoder: Decoder[TransferEvent] = deriveDecoder
  private val rawTransactionDecoder: Decoder[Transaction] = deriveDecoder
  private implicit val transactionDecoder: Decoder[Transaction] = Decoder.decodeJson.map { j =>
    val newJ = j.hcursor.downField("block").withFocus(_.withObject(jo => jo.add("txs", Array().asJson).asJson)).top.get
    newJ.hcursor.downField("transfer_events").withFocus(j => j.hcursor.downField("list").focus.get).top.get
  }.map(_.as[Transaction](rawTransactionDecoder) match {
    case Right(r) => r
    case Left(e) => throw e
  })

  def currentBlock: IO[Block] = {
    val uri = baseUrl / "blocks" / "current"
    val request = Request[IO](uri = uri)
    client.expect[Block](request)
  }

  def blockByHash(hash: Hex): IO[Block] = {
    val uri = baseUrl / "blocks" / hash.hexString
    val request = Request[IO](uri = uri)
    client.expect[Json](request).map(_.hcursor.downN(0).as[Block] match {
      case Right(t) => t
      case Left(e) => throw e
    })
  }

  def blockByHeight(height: Int): IO[Block] = {
    val uri = baseUrl / "blocks" / height.toString
    val request = Request[IO](uri = uri)
    client.expect[Json](request).map(_.hcursor.downN(0).as[Block] match {
      case Right(t) => t
      case Left(e) => throw e
    })
  }

  def transactionByHash(hash: Hex): IO[Transaction] = {
    val uri = baseUrl / "transactions" / hash.hexString
    val request = Request[IO](uri = uri)
    client.expect[Transaction](request)
  }

  def transactionsByAddress(address: Hex): IO[List[Transaction]] = {
    val uri = baseUrl / "addresses" / address.hexString / "transactions" +? ("no_token", true)
    val request = Request[IO](uri = uri)
    client.expect[Json](request).map(_.hcursor.get[List[Transaction]]("txs").right.get)
  }

  def addressBalance(address: Hex): IO[Balance] = {
    val uri = baseUrl / "addresses" / address.hexString / "balance"
    val request = Request[IO](uri = uri)
    client.expect[Json](request).map{_.hcursor.downN(0).as[Balance].right.get }
  }
}
