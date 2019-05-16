package co.ledger.blockchain.graphql.model

import java.time.Instant

import co.ledger.blockchain.graphql.utils.crypto.Hex

object EthSchema {

  case class Schema (
    query: Query
  )

  case class Query (
    blocks: List[Block],
    addresses: List[Address],
    transactions: List[Transaction]
  )

  case class Block (
    hash: Hex,
    height: Int,
    time: Instant,
    transactions: List[Hex] // Should be [Transactions]
)

  case class Address (
    balance: BigInt,
    nonce: Int,
    transactions: List[Transaction],
    pendingTransactions: List[PendingTransaction],
    estimatedGasLimit: BigInt
  )

  case class Transaction (
    hash: Hex,
    status: Int,
    receivedAt: Instant,
    nonce: Int,
    value: BigInt,
    gas: BigInt,
    gasPrice: BigInt,
    from: Hex,
    to: Hex,
    input: Hex,
    cumulativeGasUsed: BigInt,
    gasUsed: BigInt,
    transferEvents: List[TransferEvent],
    block: Block
  )

  case class TransferEvent (
    contract: Hex,
    from: Hex,
    to: Hex,
    count: Int,
    decimal: Int,
    symbol: String
  )

  case class PendingTransaction (
    hash: Hex,
    nonce: Int,
    value: BigInt,
    gas: BigInt,
    gasPrice: BigInt,
    from: Hex,
    to: Hex,
    input: Hex
  )
}