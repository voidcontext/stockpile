package mtg.stockpile

import cats.kernel.Monoid

object stockpile2 {
  sealed trait Card

  trait CardList[A <: Card] {
    def cards: Set[A]
  }

  sealed trait DeckSlot
  case object MainBoard extends DeckSlot
  case object SideBoard extends DeckSlot
  case object MaybeBoard extends DeckSlot

  trait Deck[A <: Card] {
    def mainBoard: CardList[A]
    def sideBoard: CardList[A]
    def maybeBoard: CardList[A]
  }

  implicit def cardListMonoid[A <: Card]: Monoid[CardList[A]] = new Monoid[CardList[A]] {
    override def empty: CardList[A] = new CardList[A] {
      def cards = Set.empty
    }

    override def combine(
      x: CardList[A],
      y: CardList[A]
    ): CardList[A] = new CardList[A] {
      def cards = x.cards.union(y.cards)
    }
  }

}
