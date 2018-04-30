package vdx.stockpile

import cats.Eq
import cats.data.Writer

final case class Deck[A <: Card[A]](
  mainBoard: CardList[A] = CardList.empty[A],
  sideBoard: CardList[A] = CardList.empty[A],
  maybeBoard: CardList[A] = CardList.empty[A]
) {
  def toCardList(implicit eq: Eq[A]): CardList[A] = mainBoard.combine(sideBoard).combine(maybeBoard)

}

object Deck {

  def apply[A <: Card[A]](
    mainBoard: CardList[A] = CardList.empty[A],
    sideBoard: CardList[A] = CardList.empty[A],
    maybeBoard: CardList[A] = CardList.empty[A]
  ): Deck[A] = new Deck(mainBoard, sideBoard, maybeBoard)

  sealed trait DeckLog {
    def message: String
  }

  case class ParserError(message: String) extends DeckLog

  type DeckLoaderResult[A <: Card[A]] = Writer[Vector[DeckLog], Deck[A]]

  trait DeckLoaderAlg[F[_], A <: Card[A]] {
    def load: F[DeckLoaderResult[A]]
  }
}
