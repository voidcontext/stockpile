package vdx.stockpile.cardlist

import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.{DeckListCard, Edition, InventoryCard, NonFoil}
import vdx.stockpile._

class CardListOperationsSpec extends FlatSpec with Matchers with CardListOperations {

  val deck: DeckList = Deck(
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

  "Difference[DeckListCard, InventoryCard, DeckListCard]" should "implement the 'cards missing' feature" in {
    val instance = implicitly[Difference[DeckListCard, InventoryCard, DeckListCard]]

    instance(deck.toCardList, inventory).toList should equal(
      CardList(DeckListCard("Reality Smasher", 2), DeckListCard("Eldrazi Temple", 4)).toList
    )
  }

  "Intersection[DeckListCard, InventoryCard, DeckListCard]" should "implement the 'cards that I have' feature" in {
    val instance = implicitly[Intersection[DeckListCard, InventoryCard, DeckListCard]]

    instance(deck.toCardList, inventory).toList should equal(
      CardList(DeckListCard("Thought-Knot Seer", 4), DeckListCard("Reality Smasher", 2)).toList
    )
  }
}
