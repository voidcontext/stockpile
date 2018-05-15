package vdx.stockpile.pricing

import cats.Id
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card
import vdx.stockpile.Card.DeckListCard

class PricerSpec extends FlatSpec with Matchers {
  "Pricer" should "provide price for a DecklistCard" in {
    implicit val pricer = new PricerAlg[Id] {
      override def getPrice[A <: Card[A]](card: A): Id[CardPrice[A]] = card match {
        case c: DeckListCard => CardPrice(card, Price(56.30, EUR, Cardmarket))
      }

    }

    DeckListCard("Tarmogoyf", 1).price shouldBe a[CardPrice[DeckListCard]]
  }
}
