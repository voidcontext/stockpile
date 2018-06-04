package vdx.stockpile.cardlist

import cats.Eq
import cats.instances.AllInstances
import cats.kernel.laws.discipline.MonoidTests
import cats.syntax.AllSyntax
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline
import vdx.stockpile.Card
import vdx.stockpile.Card._
import vdx.stockpile.instances.eq._

class CardListMonoidSpec
    extends FunSuite
    with Discipline
    with CardListSuite
    with AllSyntax
    with AllInstances
    with CardListInstances {
  implicit def cardListEq[A <: Card[A]](implicit eq: Eq[A]): Eq[CardList[A]] = { (x: CardList[A], y: CardList[A]) =>
    cardListFoldable.forall(x)(
      card => cardListFoldable.exists(y)(other => eq.eqv(card, other) && card.count == other.count)
    )
  }

  checkAll("DeckListCardMonoid", MonoidTests(deckListCardListMonoid).monoid)

  checkAll("InvenotryCardMonoid", MonoidTests(inventoryCardCardListMonoid).monoid)
}
