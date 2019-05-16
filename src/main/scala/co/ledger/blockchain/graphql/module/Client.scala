package co.ledger.blockchain.graphql.module

import cats.effect.{ExitCode, IO, IOApp}
import co.ledger.blockchain.graphql.model.EthSchema.{Block, Transaction, TransferEvent}
import co.ledger.blockchain.graphql.utils.crypto.Hex
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient

class LedgerExplorerETHClient(client: Client[IO]) {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
  private val baseUrl = uri"http://eth-mainnet.explorers.dev.aws.ledger.fr/blockchain/v3/"
  private implicit val blockDecoder: Decoder[Block] = deriveDecoder
  private implicit val blockEncoder: Encoder[Block] = deriveEncoder
  implicit val teDecoder: Decoder[TransferEvent] = deriveDecoder
  private val rawTransactionDecoder: Decoder[Transaction] = deriveDecoder
  private implicit val transactionDecoder: Decoder[Transaction] = Decoder.decodeJson.map { j =>
    val newJ = j.hcursor.downField("block").withFocus(_.withObject(jo => jo.add("txs", Array().asJson).asJson)).top.get
    newJ.hcursor.downField("transfer_events").withFocus(j => j.hcursor.downField("list").focus.get).top.get
  }.map(_.as[Transaction](rawTransactionDecoder) match {
    case Right(r) => r
    case Left(e) => throw e
  })

  def blockByHash(hash: Hex): IO[Block] = {
    val uri = baseUrl / "blocks" / hash.hexString
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

  def addressBalance(address: Hex): IO[BigInt] = {
    val uri = baseUrl / "addresses" / address.hexString / "balance"
    val request = Request[IO](uri = uri)
    client.expect[Json](request).map{_.hcursor.downN(0).get[BigInt]("balance").right.get }
  }
}

object Test extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    AsyncHttpClient.resource[IO]().use{ c =>
      val client = new LedgerExplorerETHClient(c)
      for {
        block <- client.blockByHash(Hex.fromString("0x1ecde16273f6506a6042fe305e60a544325f5a3dae9996588f567e16173b68ba").right.get)
        _ <- IO(println(block))
        transaction <- client.transactionByHash(Hex.fromString("0x38430292b85cf9727205a32700ba378f3e641aa08c13a961851cbbb76622b673").right.get)
        _ <- IO(println(transaction))
        txs <- client.transactionsByAddress(Hex.fromString("0x28e8b0d8ebac4175dfaeb64f71537bb00d8d0de1").right.get)
        _ <- IO(println(txs))
        balance <- client.addressBalance(Hex.fromString("0x28e8b0d8ebac4175dfaeb64f71537bb00d8d0de1").right.get)
        _ <- IO(println(balance))
      } yield ExitCode.Success
    }
  }
}
