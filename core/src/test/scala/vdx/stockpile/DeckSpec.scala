package vdx.stockpile

import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.instances.eq._
import cats.syntax.foldable._
import cats.syntax.monoid._
import vdx.stockpile.cardlist.{CardList, CardListInstances}

class DeckSpec extends FlatSpec with Matchers with CardListInstances {
  val cardTarmogoyf = DeckListCard("Tarmogoyf", 4)

  "Deck()" should "create an empty deck" in {
    val deck = Deck[DeckListCard]("dummy deck")

    deck.mainBoard.isEmpty shouldBe true
    deck.sideBoard.isEmpty shouldBe true
    deck.maybeBoard.isEmpty shouldBe true
  }

  "Deck(...)" should "import mainboard cards when set" in {
    val deck = Deck("dummy deck", mainBoard = CardList(cardTarmogoyf))

    deck.mainBoard.exists(_ === cardTarmogoyf) shouldBe true
    deck.sideBoard.isEmpty shouldBe true
    deck.maybeBoard.isEmpty shouldBe true
  }

  it should "import sideboard cards when set" in {
    val deck = Deck("dummy deck", sideBoard = CardList(cardTarmogoyf))

    deck.mainBoard.isEmpty shouldBe true
    deck.sideBoard.exists(_ === cardTarmogoyf) shouldBe true
    deck.maybeBoard.isEmpty shouldBe true
  }

  it should "import maybeboard cards when set" in {
    val deck = Deck("dummy deck", maybeBoard = CardList(cardTarmogoyf))

    deck.mainBoard.isEmpty shouldBe true
    deck.sideBoard.isEmpty shouldBe true
    deck.maybeBoard.exists(_ === cardTarmogoyf) shouldBe true
  }
}
