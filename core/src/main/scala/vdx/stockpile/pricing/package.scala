package vdx.stockpile

import cats.Monad
import cats.effect.IO

package object pricing {

  sealed trait Currency
  case object EUR extends Currency
  case object USD extends Currency

  sealed trait Source
  case object Cardmarket extends Source

  trait PricerAlg[F[_]] {
    def getPrice[A <: Card[A]](card: A): F[CardPrice[A]]
  }

  case class Price(
    amount: Double,
    currency: Currency,
    source: Source
  )

  case class CardPrice[A <: Card[A]](
    card: A,
    price: Price,
  )

  implicit class CardPricingOps[A <: Card[A], F[_]: Monad](card: A) {
    def price(implicit p: PricerAlg[F]): F[CardPrice[A]] = p.getPrice(card)
  }
}
