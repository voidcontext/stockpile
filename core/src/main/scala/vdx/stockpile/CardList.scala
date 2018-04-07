package vdx.stockpile

import cats.{Eq, Eval, Foldable}

sealed trait CardList[A <: Card[A]] {
  def contains(card: A)(implicit eq: Eq[A]): Boolean

  def add(card: A)(implicit eq: Eq[A]): CardList[A]

  def foldLeft[B](z: B)(f: (B, A) => B): B

  def toList: List[A] = foldLeft(List.empty[A])(_ :+ _)
}

object CardList {

  def apply[A <: Card[A]](): CardList[A] = empty

  def empty[A <: Card[A]]: CardList[A] = new CardListStrict[A](List.empty)

  private case class CardListStrict[A <: Card[A]](cards: List[A]) extends CardList[A] {

    override def contains(card: A)(implicit eq: Eq[A]): Boolean = cards.exists(eq.eqv(_, card))

    override def add(card: A)(implicit eq: Eq[A]): CardList[A] = {
      new CardListStrict[A](
        if (contains(card)) {
          cards.map(current => if (eq.eqv(current, card)) card.withCount(card.count + current.count) else card)
        } else {
          cards :+ card
        }
      )
    }

    override def foldLeft[B](z: B)(f: (B, A) => B): B = cards.foldLeft(z)(f)
  }
}
