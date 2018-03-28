package vdx.stockpile

import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.instances._

class DeckSpec extends FlatSpec with Matchers {
  val cardTarmogoyf = DeckListCard("Tarmogoyf", 4)

  "Deck()" should "create an empty deck" in {
    val deck = Deck()

    deck.mainBoard.toList shouldBe empty
    deck.sideBoard.toList shouldBe empty
    deck.maybeBoard.toList shouldBe empty
  }

  "Deck(...)" should "import mainboard cards when set" in {
    val deck = Deck(mainBoard = CardList().add(cardTarmogoyf))

    deck.mainBoard.contains(cardTarmogoyf) shouldBe true
    deck.sideBoard.toList shouldBe empty
    deck.maybeBoard.toList shouldBe empty
  }

  it should "import sideboard cards when set" in {
    val deck = Deck(sideBoard = CardList().add(cardTarmogoyf))

    deck.mainBoard.toList shouldBe empty
    deck.sideBoard.contains(cardTarmogoyf) shouldBe true
    deck.maybeBoard.toList shouldBe empty
  }

  it should "import maybeboard cards when set" in {
    val deck = Deck(maybeBoard = CardList().add(cardTarmogoyf))

    deck.mainBoard.toList shouldBe empty
    deck.sideBoard.toList shouldBe empty
    deck.maybeBoard.contains(cardTarmogoyf) shouldBe true
  }
}
