package vdx.stockpile.pricing

import cats.Id
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card
import vdx.stockpile.Card.DeckListCard

class PricerSpec extends FlatSpec with Matchers {
  "Pricer package" should "provide pricing syntactic sugar" in {
    def priceOf[A <: Card[A]](card: A): CardPrice[A] = CardPrice(card, Price(56.30, EUR, Cardmarket))

    implicit val pricer = new PricerAlg[Id] {
      override def getPrice[A <: Card[A]](card: A): Id[CardPrice[A]] = card match {
        case c: DeckListCard => priceOf(card)
      }

    }

    val card = DeckListCard("Tarmogoyf", 1)
    card.price should equal(priceOf(card))
  }
}
