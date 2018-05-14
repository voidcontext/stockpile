package vdx.stockpile

import vdx.stockpile.Card.InventoryCard

package object pricing {

  sealed trait Currency

  sealed trait Source

  trait PricerAlg[F[_]] {
    def getPrice(card: InventoryCard): F[CardPrice]
  }

  case class Price(
    amount: Double,
    currency: Currency,
    source: Source
  )

  case class CardPrice(
    card: InventoryCard,
    price: Price,
  )
}
