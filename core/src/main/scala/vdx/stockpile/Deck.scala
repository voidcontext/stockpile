package vdx.stockpile

final case class Deck[A <: Card[A]](
  mainBoard: CardList[A] = CardList.empty[A],
  sideBoard: CardList[A] = CardList.empty[A],
  maybeBoard: CardList[A] = CardList.empty[A]
)
