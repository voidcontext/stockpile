package vdx.stockpile.cardlist

import cats.{Eq, Monoid}
import vdx.stockpile.Card

class CardListMonoid[A <: Card[A]](implicit eq: Eq[A]) extends Monoid[CardList[A]] {
  import CardListMonoid._

  override def empty: CardList[A] = CardList(Set.empty[A])

  override def combine(
    x: CardList[A],
    y: CardList[A]
  ): CardList[A] = y.cards.foldLeft(x) { (list, card) =>
    def appendTo(set: Set[A], card: A) = {
      set.foldLeft(CombineResult(Set.empty[A], Some(card))) { (result, card) =>
        val (toAdd, remaining) = result.card
          .map { toAdd =>
            if (eq.eqv(toAdd, card)) (card.withCount(card.count + toAdd.count), None)
            else (card, Some(toAdd))
          }
          .getOrElse((card, None))

        CombineResult(result.list + toAdd, remaining)
      }
    }

    CardList(
      appendTo(list.cards, card) match {
        case CombineResult(l, Some(c)) => l + c
        case CombineResult(l, _)       => l
      }
    )
  }
}

object CardListMonoid {
  private[cardlist] case class CombineResult[A <: Card[A]](list: Set[A], card: Option[A])
}
