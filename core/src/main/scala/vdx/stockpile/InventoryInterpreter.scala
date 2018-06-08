package vdx.stockpile

import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.cardlist.CardList

trait InventoryInterpreter extends InventoryAlgebra[Inventory, DeckList, BuiltDeck] {
  import cats.syntax.foldable._
  import cats.syntax.monoid._
  import vdx.stockpile.instances.cardlist._

  override def cardsOwned(inventory: Inventory, deckList: DeckList): DeckList = {
    deckList.foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
      val totalNumOfHaves = inventory.filter_(_.name == card.name).map(_.count).sum

      if (totalNumOfHaves > 0) list.combine(CardList(card.withCount(Math.min(card.count, totalNumOfHaves))))
      else list
    }

  }

  override def cardsToBuy(inventory: Inventory, deckList: DeckList): DeckList = {
    deckList.foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
      val diff = inventory
        .filter_(_.name == card.name)
        .foldLeft(card) { (card, inventoryCard) =>
          card.withCount(card.count - inventoryCard.count)
        }

      if (diff.count > 0) list.combine(CardList(diff))
      else list
    }
  }

}
