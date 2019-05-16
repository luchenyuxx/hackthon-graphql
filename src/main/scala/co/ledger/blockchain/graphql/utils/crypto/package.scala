package co.ledger.blockchain.graphql.utils


package object crypto {

  implicit class postfixStringOps(from: String) {
    def toHex: Either[String, Hex] = Hex.fromString(from)
  }

  implicit class postfixBytesOps(from: Array[Byte]) {
    def toHex: Hex = Hex.fromBytes(from)
  }

  implicit class postfixBigIntOps(from: BigInt) {
    def toHex: Hex = Hex.fromBigInt(from)
  }
}
