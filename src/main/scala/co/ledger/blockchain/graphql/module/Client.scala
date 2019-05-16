package co.ledger.blockchain.graphql.module

import cats.effect.IO
import co.ledger.blockchain.graphql.model.EthSchema.{Block, Transaction, TransferEvent}
import co.ledger.blockchain.graphql.utils.crypto.Hex
import io.circe.generic.extras._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client

class LedgerExplorerETHClient(client: Client[IO]) {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
  val baseUrl = uri"http://eth-mainnet.explorers.dev.aws.ledger.fr/blockchain/v3/"
  implicit val blockDecoder: Decoder[Block] = deriveDecoder
  implicit val blockEncoder: Encoder[Block] = deriveEncoder
  implicit val teDecoder: Decoder[TransferEvent] = deriveDecoder
  val rawTransactionDecoder: Decoder[Transaction] = deriveDecoder
  implicit val transactionDecoder: Decoder[Transaction] = Decoder.decodeJson.map { j =>
    val newJ = j.hcursor.downField("block").withFocus(_.withObject(jo => jo.add("txs", Array().asJson).asJson)).top.get
    newJ.hcursor.downField("transfer_events").withFocus(j => j.hcursor.downField("list").focus.get).top.get
  }.map(_.as[Transaction](rawTransactionDecoder).right.get)

  def blockByHash(hash: Hex): IO[Block] = {
    val uri = baseUrl / "blocks" / hash.hexString
    val request = Request[IO](uri = uri)
    client.expect[Block](request)
  }

  def transactionByHash(hash: Hex): IO[Transaction] = {
    val uri = baseUrl / "transactions" / hash.hexString
    val request = Request[IO](uri = uri)
    client.expect[Transaction](request)
  }

  def transactionsByAddress(address: Hex): IO[List[Transaction]] = {
    val uri = baseUrl / "addresses" / address.hexString / "transactions" +? "no_token"
    val request = Request[IO](uri = uri)
    client.expect[Json](request).map(_.hcursor.get[List[Transaction]]("txs").right.get)
  }

  def addressBalance(address: Hex): IO[BigInt] = {
    val uri = baseUrl / "addresses" / address.hexString / "balance"
    val request = Request[IO](uri = uri)
    client.expect[Json](request).map(_.hcursor.downN(0).get[BigInt]("balance").right.get)
  }
}
