package vdx.stockpile.cardlist

import cats.{Eval, Foldable}

class CardListFoldable extends Foldable[CardList] {
  override def foldLeft[A, B](fa: CardList[A], b: B)(f: (B, A) => B): B = fa.cards.foldLeft(b)(f)

  override def foldRight[A, B](fa: CardList[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
    fa.cards.foldRight(lb)(f)
}
