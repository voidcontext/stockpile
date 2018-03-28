package vdx.stockpile

import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card._
import vdx.stockpile.instances._

class CardSpec extends FlatSpec with Matchers {
  "DeckListCard.withCount" should "set count without changing other values" in {
    val card = DeckListCard("Tarmogoyf", 4)
    val oneCard = card.withCount(1)

    oneCard should equal(DeckListCard(card.name, 1))
  }

  it should "have an Eq instance to construct cardlists" in {
    val card = DeckListCard("Tarmogoyf", 4)

    CardList.empty.add(card) shouldBe an[CardList[DeckListCard]]
  }

  "InventoryCard.withCount" should "set count without changing other values" in {
    val card = InventoryCard("Tarmogoyf", 4, Edition("MM3"), NonFoil)
    val oneCard = card.withCount(1)

    oneCard should equal(InventoryCard(card.name, 1, card.edition, card.foil))
  }

  it should "have an Eq instance to construct cardlists" in {
    val card = InventoryCard("Tarmogoyf", 4, Edition("MM3"), NonFoil)

    CardList.empty.add(card) shouldBe an[CardList[InventoryCard]]
  }

  "WantsListCard.withCount" should "set count without changing other values" in {
    val card = WantsListCard("Tarmogoyf", 4, Option(Edition("MM3")), None)
    val oneCard = card.withCount(1)

    oneCard should equal(WantsListCard(card.name, 1, card.edition, card.foil))
  }

  it should "have an Eq instance to construct cardlists" in {
    val card = WantsListCard("Tarmogoyf", 4, None, None)

    CardList.empty.add(card) shouldBe an[CardList[WantsListCard]]
  }
}
