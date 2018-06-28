package vdx.stockpile

import cats.Id
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.cardlist.CardList

trait InventoryInterpreter extends InventoryAlgebra[Id, Inventory, DeckList, BuiltDeck] {
  import cats.syntax.foldable._
  import cats.syntax.monoid._
  import vdx.stockpile.instances.cardlist._

  override def cardsOwned(inventory: Inventory, deckList: DeckList): Id[DeckList] = {
    deckList.foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
      val totalNumOfHaves = inventory.filter_(_.name == card.name).map(_.count).sum

      list.combine(CardList(card.withCount(Math.min(card.count, totalNumOfHaves))))
    }
  }

  override def cardsToBuy(inventory: Inventory, deckList: DeckList): Id[DeckList] = {
    deckList.foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
      list.combine(
        CardList(
          inventory
            .filter_(_.name == card.name)
            .foldLeft(card) { (card, inventoryCard) =>
              card.withCount(card.count - inventoryCard.count)
            }
        )
      )
    }
  }
}
