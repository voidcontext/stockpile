package vdx.stockpile.cardlist

import cats.syntax.monoid._
import cats.{Eq, Monoid}
import vdx.stockpile.Card

sealed trait CardList[A] {
  private[cardlist] def cards: Set[A]
}

object CardList {
  def empty[A <: Card[A]]: CardList[A] = new CardList[A] {
    override def cards: Set[A] = Set.empty[A]
  }

  def apply[A <: Card[A]](): CardList[A] = empty
  def apply[A <: Card[A]](card: A): CardList[A] = new CardList[A] {
    override def cards: Set[A] = Set(card)
  }
  def apply[A <: Card[A]](cards: A*)(implicit eq: Eq[A], clm: Monoid[CardList[A]]): CardList[A] = {
    cards.foldLeft[CardList[A]](empty)(_ |+| apply(_))
  }

  private[cardlist] def apply[A <: Card[A]](set: Set[A]): CardList[A] = new CardList[A] {
    override private[cardlist] def cards = set
  }
}
