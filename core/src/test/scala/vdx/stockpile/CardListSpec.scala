package vdx.stockpile

import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.{DeckListCard, InventoryCard}
import vdx.stockpile.instances.eq._
import cats.syntax.monoid._
import cats.syntax.foldable._
import vdx.stockpile.cardlist.{CardList, CardListInstances}

class CardListSpec extends FlatSpec with Matchers with CardListInstances {
  val card = DeckListCard("Thought-Knot Seer", 1)

  "CardList()" should "instantiate an empty card list" in {
    val list = CardList[DeckListCard]()

    list.isEmpty should be(true)
  }

  "CardList(card)" should "add a single card" in {
    val list = CardList(card)

    (list.toList should have).length(1)
  }

  it should "add the given card" in {
    val list = CardList(card)

    list.toList should equal(List(card))
  }
  "CardList.combine()" should "combine 2 empty lists" in {
    val list = CardList[InventoryCard]() |+| CardList[InventoryCard]()

    list.isEmpty should be(true)
  }

  it should "add increase the count of a card when the same card is combined" in {
    val cards1 = List(
      DeckListCard("Path to Exile", 1),
      DeckListCard("Tarmogoyf", 4),
      DeckListCard("Thought-Knot Seer", 4)
    )
    val cards2 = List(
      DeckListCard("Eldrazi Displacer", 2),
      DeckListCard("Tarmogoyf", 4)
    )
    val list = CardList(cards1: _*).combine(CardList(cards2: _*))

    list.toList should equal(
      List(
        DeckListCard("Path to Exile", 1),
        DeckListCard("Tarmogoyf", 8),
        DeckListCard("Thought-Knot Seer", 4),
        DeckListCard("Eldrazi Displacer", 2)
      )
    )
  }

//  "Cardlist.contains(card)" should "return false when a card is not in the list" in {
//    val list = CardList[DeckListCard]()
//
//    list.contains(card) should be(false)
//  }
//
//  it should "return true when a card is in the list" in {
//    val list = CardList(card)
//
//    list.contains(card) should be(true)
//  }
}
