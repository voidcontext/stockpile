package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import vdx.stockpile.{Card, pricing}
import vdx.stockpile.pricing.PricerAlg

class Pricer extends PricerAlg[IO] {
  override def getPrice(card: Card.InventoryCard): IO[pricing.CardPrice] = ???
}
