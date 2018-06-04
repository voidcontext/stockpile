package vdx.stockpile.cardlist

import vdx.stockpile.Card.{DeckListCard, InventoryCard}
import vdx.stockpile.Card
import cats.syntax.foldable._
import cats.syntax.monoid._

trait CardListOperations {
  sealed trait Difference[A <: Card[A], B <: Card[B], C <: Card[C]] {
    def apply(a: CardList[A], b: CardList[B])(
      implicit clf: CardListFoldable,
      clm: CardListMonoid[DeckListCard]
    ): CardList[C]
  }
  sealed trait Intersection[A <: Card[A], B <: Card[B], C <: Card[C]] {
    def apply(a: CardList[A], b: CardList[B])(
      implicit clf: CardListFoldable,
      clm: CardListMonoid[DeckListCard]
    ): CardList[C]
  }

  implicit object MissingFromDeck extends Difference[DeckListCard, InventoryCard, DeckListCard] {
    override def apply(
      a: CardList[DeckListCard],
      b: CardList[InventoryCard]
    )(implicit clf: CardListFoldable, clm: CardListMonoid[DeckListCard]): CardList[DeckListCard] =
      a.foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
        val diff = b
          .filter_(_.name == card.name)
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
    )(implicit clf: CardListFoldable, clm: CardListMonoid[DeckListCard]): CardList[DeckListCard] =
      a.foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
        val totalNumOfHaves = b.filter_(_.name == card.name).map(_.count).sum

        if (totalNumOfHaves > 0) list.combine(CardList(card.withCount(Math.min(card.count, totalNumOfHaves))))
        else list
      }
  }

}
