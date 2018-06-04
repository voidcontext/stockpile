package vdx.stockpile.cardlist

import cats.laws.discipline.FoldableTests
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.instances.cardlist.cardListFoldable

class CardListFoldableSpec extends FunSuite with Discipline with CardListSuite {
//  checkAll("CardListFoldable", FoldableTests[CardList].foldable[DeckListCard, DeckListCard])
}
