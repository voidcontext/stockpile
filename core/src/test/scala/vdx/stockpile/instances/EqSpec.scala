package vdx.stockpile.instances

import cats.Eq
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card._
import vdx.stockpile.instances.eq._

class EqSpec extends FlatSpec with Matchers {
  "Eq[Edition]" should "be an implicit value" in {
    val eq = implicitly[Eq[Edition]]

    eq shouldBe an[Eq[Edition]]
  }

  it should "return true when codes are the same" in {
    val eq = implicitly[Eq[Edition]]

    eq.eqv(Edition("MM3"), Edition("MM3")) should be(true)
  }

  it should "return false when codes aren't the same" in {
    val eq = implicitly[Eq[Edition]]

    eq.eqv(Edition("MM3"), Edition("CON")) should be(false)
  }

  "Eq[DeckListCard]" should "be an implicit value" in {
    val eq = implicitly[Eq[DeckListCard]]

    eq shouldBe an[Eq[DeckListCard]]
  }

  it should "return true when card names are the same" in {
    val eq = implicitly[Eq[DeckListCard]]

    val card1 = DeckListCard("Tarmogoyf", 1)
    val card2 = DeckListCard("Tarmogoyf", 2)

    eq.eqv(card1, card2) should be(true)
  }

  it should "return false when card names are different" in {
    val eq = implicitly[Eq[DeckListCard]]

    val card1 = DeckListCard("Path to Exile", 2)
    val card2 = DeckListCard("Tarmogoyf", 2)

    eq.eqv(card1, card2) should be(false)
  }

  "Eq[InventoryCard]" should "be an implicit value" in {
    val eq = implicitly[Eq[InventoryCard]]

    eq shouldBe an[Eq[InventoryCard]]
  }

  it should "return true when name, edition and foil state are matching" in {
    val eq = implicitly[Eq[InventoryCard]]

    val card1 = InventoryCard("Tarmogoyf", 1, Edition("MM3"), NonFoil)
    val card2 = InventoryCard("Tarmogoyf", 2, Edition("MM3"), NonFoil)

    eq.eqv(card1, card2) should be(true)
  }

  it should "return false when name is different" in {
    val eq = implicitly[Eq[InventoryCard]]

    val card1 = InventoryCard("Path to Exile", 1, Edition("MM3"), NonFoil)
    val card2 = InventoryCard("Tarmogoyf", 1, Edition("MM3"), NonFoil)

    eq.eqv(card1, card2) should be(false)
  }

  it should "return false when edition is different" in {
    val eq = implicitly[Eq[InventoryCard]]

    val card1 = InventoryCard("Tarmogoyf", 1, Edition("MMA"), NonFoil)
    val card2 = InventoryCard("Tarmogoyf", 1, Edition("MM3"), NonFoil)

    eq.eqv(card1, card2) should be(false)
  }

  it should "return false when foil state is different" in {
    val eq = implicitly[Eq[InventoryCard]]

    val card1 = InventoryCard("Tarmogoyf", 1, Edition("MM3"), Foil)
    val card2 = InventoryCard("Tarmogoyf", 1, Edition("MM3"), NonFoil)

    eq.eqv(card1, card2) should be(false)
  }

  "Eq[WantsListCard]" should "be an implicit value" in {
    val eq = implicitly[Eq[WantsListCard]]

    eq shouldBe an[Eq[WantsListCard]]
  }

  it should "return true when name, edition, and foil state are the same" in {
    val eq = implicitly[Eq[WantsListCard]]

    val card1 = WantsListCard("Tarmogoyf", 1, Option(Edition("MM3")), Option(NonFoil))
    val card2 = WantsListCard("Tarmogoyf", 2, Option(Edition("MM3")), Option(NonFoil))

    eq.eqv(card1, card2) should be(true)
  }

  it should "return false when name differs" in {
    val eq = implicitly[Eq[WantsListCard]]

    val card1 = WantsListCard("Path to Exile", 1, Option(Edition("MM3")), Option(NonFoil))
    val card2 = WantsListCard("Tarmogoyf", 2, Option(Edition("MM3")), Option(NonFoil))

    eq.eqv(card1, card2) should be(false)
  }

  it should "return false when edition differs" in {
    val eq = implicitly[Eq[WantsListCard]]

    val card1 = WantsListCard("Tarmogoyf", 1, Option(Edition("MMA")), Option(NonFoil))
    val card2 = WantsListCard("Tarmogoyf", 2, Option(Edition("MM3")), Option(NonFoil))

    eq.eqv(card1, card2) should be(false)
  }

  it should "return false when only 1 of the cards' edition is not empty" in {
    val eq = implicitly[Eq[WantsListCard]]

    val card1 = WantsListCard("Tarmogoyf", 1, Option(Edition("MMA")), Option(NonFoil))
    val card2 = WantsListCard("Tarmogoyf", 2, None, Option(NonFoil))

    eq.eqv(card1, card2) should be(false)
  }

  it should "return false when foil state differs" in {
    val eq = implicitly[Eq[WantsListCard]]

    val card1 = WantsListCard("Tarmogoyf", 1, Option(Edition("MM3")), Option(Foil))
    val card2 = WantsListCard("Tarmogoyf", 2, Option(Edition("MM3")), Option(NonFoil))

    eq.eqv(card1, card2) should be(false)
  }

  it should "return false when 1 of the cards' foil state is not empty" in {
    val eq = implicitly[Eq[WantsListCard]]

    val card1 = WantsListCard("Tarmogoyf", 1, Option(Edition("MM3")), Option(Foil))
    val card2 = WantsListCard("Tarmogoyf", 2, Option(Edition("MM3")), None)

    eq.eqv(card1, card2) should be(false)
  }
}
