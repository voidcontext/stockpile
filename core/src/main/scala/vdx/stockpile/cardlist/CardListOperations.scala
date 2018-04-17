package vdx.stockpile.cardlist

import vdx.stockpile.Card.{DeckListCard, InventoryCard}
import vdx.stockpile.instances.{EqInstances}
import vdx.stockpile.{Card, CardList}

trait CardListOperations extends EqInstances {
  sealed trait Difference[A <: Card[A], B <: Card[B], C <: Card[C]] {
    def apply(a: CardList[A], b: CardList[B]): CardList[C]
  }
  sealed trait Intersection[A <: Card[A], B <: Card[B], C <: Card[C]] {
    def apply(a: CardList[A], b: CardList[B]): CardList[C]
  }

  implicit object MissingFromDeck extends Difference[DeckListCard, InventoryCard, DeckListCard] {
    override def apply(
      a: CardList[DeckListCard],
      b: CardList[InventoryCard]
    ): CardList[DeckListCard] = a.foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
      val diff = b
        .filter(_.name == card.name)
        .foldLeft(card) { (card, inventoryCard) =>
          card.withCount(card.count - inventoryCard.count)
        }

      if (diff.count > 0) list.combine(CardList(diff))
      else list
    }
  }

  implicit object HavesInDeck extends Intersection[DeckListCard, InventoryCard, DeckListCard] {
    override def apply(
      a: CardList[DeckListCard],
      b: CardList[InventoryCard]
    ): CardList[DeckListCard] = ???
  }

}
