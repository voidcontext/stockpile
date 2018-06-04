package vdx.stockpile

import cats.Eq
import cats.data.Writer
import cats.syntax.monoid._
import vdx.stockpile.cardlist.{CardList, CardListMonoid}

final case class Deck[A <: Card[A]](
  name: String,
  mainBoard: CardList[A] = CardList.empty[A],
  sideBoard: CardList[A] = CardList.empty[A],
  maybeBoard: CardList[A] = CardList.empty[A]
) {
  def toCardList(implicit eq: Eq[A], clm: CardListMonoid[A]): CardList[A] =
    mainBoard |+| sideBoard |+| maybeBoard
}

object Deck {

  def apply[A <: Card[A]](
    name: String,
    mainBoard: CardList[A] = CardList.empty[A],
    sideBoard: CardList[A] = CardList.empty[A],
    maybeBoard: CardList[A] = CardList.empty[A]
  ): Deck[A] = new Deck(name, mainBoard, sideBoard, maybeBoard)

  sealed trait DeckLog {
    def message: String
  }

  case class ParserError(message: String) extends DeckLog

  type DeckLoaderResult[A <: Card[A]] = Writer[Vector[DeckLog], Deck[A]]

  trait DeckLoaderAlg[F[_], A <: Card[A]] {
    def load: F[DeckLoaderResult[A]]
  }
}
