package vdx.stockpile

import cats.syntax.all._
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.{DeckListCard, Edition, InventoryCard, NonFoil}
import vdx.stockpile.cardlist.CardList
import vdx.stockpile.instances.eq._

class DeckInterpreterSpec extends FlatSpec with Matchers with DeckInterpreter {
  val deck: Deck[DeckListCard] = Deck(
    "dummy deck",
    mainBoard = CardList(
      DeckListCard("Thought-Knot Seer", 4),
      DeckListCard("Reality Smasher", 4),
      DeckListCard("Eldrazi Temple", 4),
    )
  )

  val inventory: Inventory = CardList(
    InventoryCard("Noble Hierarch", 2, Edition("MM2"), NonFoil),
    InventoryCard("Thought-Knot Seer", 4, Edition("BFZ"), NonFoil),
    InventoryCard("Reality Smasher", 2, Edition("BFZ"), NonFoil),
  )

  "cardsOwned" should "return the owned cards" in {
    cardsOwned(inventory)(deck.toCardList).extract.toList should equal(
      CardList(DeckListCard("Thought-Knot Seer", 4), DeckListCard("Reality Smasher", 2)).toList
    )
  }

  "cardsToBuy" should "return the missing cards" in {
    cardsToBuy(inventory)(deck.toCardList).extract.toList should equal(
      CardList(DeckListCard("Reality Smasher", 2), DeckListCard("Eldrazi Temple", 4)).toList
    )
  }

}
