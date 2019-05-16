package co.ledger.blockchain.graphql.model

import java.time.Instant

import co.ledger.blockchain.graphql.utils.crypto.Hex

object EthSchema {

  case class Block(
                    hash: Hex,
                    height: Int,
                    time: Instant,
                    txs: List[Hex] // Should be [Transactions]
                  )

  case class Balance(
                      balance: BigInt,
                      address: Hex,
                    )

  case class Transaction(
                          hash: Hex,
                          status: Int,
                          receivedAt: Instant,
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

  case class TransferEvent(
                            contract: Hex,
                            from: Hex,
                            to: Hex,
                            count: BigInt,
                            decimal: Option[Int],
                            symbol: Option[String]
                          )


}
