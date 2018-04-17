package vdx.stockpile.cardlist

import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.{DeckListCard, Edition, InventoryCard, NonFoil}
import vdx.stockpile._

class CardListOperationsSpec extends FlatSpec with Matchers with CardListOperations {

  val deck: DeckList = Deck(
    mainBoard = CardList(
      DeckListCard("Thought-Knot Seer", 4),
      DeckListCard("Reallity Smasher", 4),
      DeckListCard("Eldrazi Temple", 4),
    )
  )

  val inventory: Inventory = CardList(
    InventoryCard("Noble Hierarch", 2, Edition("MM2"), NonFoil),
    InventoryCard("Thought-Knot Seer", 4, Edition("BFZ"), NonFoil),
    InventoryCard("Reallity Smasher", 2, Edition("BFZ"), NonFoil),
  )

  "Difference[DeckListCard, InventoryCard, DeckListCard]" should "implement the 'cards missing' feature" in {
    val instance = implicitly[Difference[DeckListCard, InventoryCard, DeckListCard]]

    instance(deck.toCardList, inventory).toList should equal(
      CardList(DeckListCard("Reallity Smasher", 2), DeckListCard("Eldrazi Temple", 4)).toList
    )
  }
}
