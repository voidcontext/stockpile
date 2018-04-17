package vdx.stockpile

import cats.Eq

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
}
