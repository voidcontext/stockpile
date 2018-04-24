package vdx.stockpile

import cats.Eq
import vdx.stockpile.cardlist.CardListOperations

sealed trait CardList[A <: Card[A]] {
  def contains(card: A)(implicit eq: Eq[A]): Boolean

  def combine(other: CardList[A])(implicit eq: Eq[A]): CardList[A]

  def toList: List[A]

  def foldLeft[B](z: B)(f: (B, A) => B): B = toList.foldLeft(z)(f)

  def filter(pred: A => Boolean)(implicit eq: Eq[A]): CardList[A] = foldLeft(CardList.empty[A]) { (list, card) =>
    if (pred(card)) list.combine(CardList(card)) else list
  }
}

object CardList extends CardListOperations {
  def empty[A <: Card[A]]: CardList[A] = new CardListStrict[A](Vector.empty)
  def apply[A <: Card[A]](): CardList[A] = empty

  def apply[A <: Card[A]](card: A): CardList[A] = new CardListStrict[A](Vector(card))

  def apply[A <: Card[A]](cards: A*)(implicit eq: Eq[A]): CardList[A] = cards.foldLeft(empty[A]) { (list, card) =>
    list.combine(apply(card))
  }

  private class CardListStrict[A <: Card[A]](cards: Seq[A]) extends CardList[A] { self =>

    override def contains(card: A)(implicit eq: Eq[A]): Boolean = cards.exists(eq.eqv(_, card))

    override def combine(other: CardList[A])(implicit eq: Eq[A]): CardList[A] =
      other.foldLeft(self) { (list, card) =>
        list.add(card)
      }

    private def add(card: A)(implicit eq: Eq[A]): CardListStrict[A] = {
      new CardListStrict[A](
        if (contains(card)) {
          cards.map(
            current =>
              if (eq.eqv(current, card)) current.withCount(card.count + current.count)
              else current
          )
        } else {
          cards :+ card
        }
      )
    }

    override def toList: List[A] = cards.toList
  }
}
