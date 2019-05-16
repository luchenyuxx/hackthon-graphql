package co.ledger.blockchain.graphql.utils.crypto

import cats.instances.list._
import cats.instances.try_._
import cats.syntax.either._
import cats.kernel.{Eq, Monoid}
import cats.syntax.traverse._
import io.circe.{Decoder, Encoder}

import scala.util.Try
import scala.util.hashing.MurmurHash3

case class Hex(bytes: Array[Byte]) {

  def hexString: String = "0x" + bytes.map("%02x" format _).mkString

  def size: Int = bytes.length

  def nonEmpty: Boolean = bytes.nonEmpty

  override def toString: String = hexString

  override def hashCode: Int = MurmurHash3.arrayHash(bytes)

  override def equals(obj: Any): Boolean = obj match {
    case h: Hex => java.util.Arrays.equals(bytes, h.bytes)
    case _ => false
  }
}

object Hex {

  private val SLIDING_SIZE = 2
  private val PARSE_RADIX = 16

  def apply(from: String): Either[String, Hex] = fromString(from)

  def apply(from: BigInt): Hex = fromBigInt(from)

  implicit def fromString(from: String): Either[String, Hex] = {
    if (from == null) {
      // We have to handle this case because of java libs returning null strings.
      Right(Hex.empty)
    } else if (from.length % SLIDING_SIZE != 0) {
      // Required for the next step to be valid.
      Left(s"$from is not a valid hex string: length is not even")
    } else {
      from
        .replaceFirst("0x", "")
        .sliding(SLIDING_SIZE, SLIDING_SIZE).toList
        .map(c => Try(Integer.parseInt(c, PARSE_RADIX).toByte)).sequence
        .map(b => Hex(b.toArray))
        .toEither.leftMap(e => s"$from is not a valid hex string: ${e.getMessage}")
    }
  }

  implicit def fromBytes(bytes: Array[Byte]): Hex = Hex(bytes)

  implicit def fromBigInt(number: BigInt): Hex = {
    number.toByteArray.toList match {
      case 0 :: bytes => Hex(bytes.toArray) // Happens when the first bit is 1.
      case bytes => Hex(bytes.toArray)
    }
  }

  implicit def bytes(hex: Hex): Array[Byte] = hex.bytes

  val empty: Hex = Hex.fromBytes(Array.empty)

  implicit val ordering: Ordering[Hex] = (x: Hex, y: Hex) => x.hexString.compare(y.hexString)

  implicit val monoid: Monoid[Hex] = new Monoid[Hex] {

    override def empty: Hex = Hex.empty

    override def combine(x: Hex, y: Hex): Hex = Hex.fromBytes(x.bytes ++ y.bytes)
  }

  implicit val eq: Eq[Hex] = (x: Hex, y: Hex) => x == y
  implicit val decoder: Decoder[Hex] = Decoder.decodeString.emap(fromString)
  implicit val encoder: Encoder[Hex] = Encoder.encodeString.contramap(_.hexString)
}
