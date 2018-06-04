package vdx.stockpile.cardlist

import org.scalacheck.{Arbitrary, Gen}
import vdx.stockpile.Card._
import vdx.stockpile.instances.eq._

trait CardListSuite {
  implicit val arbitraryDeckListCard = Arbitrary(
    Gen.oneOf(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 2))
  )

  implicit val arbitraryInventoryCard = Arbitrary(
    Gen.oneOf(
      InventoryCard("Tarmogoyf", 4, Edition("MM3"), Foil),
      InventoryCard("Path to Exile", 2, Edition("CON"), NonFoil)
    )
  )

  implicit val arbitraryCardListDeckListCard: Arbitrary[CardList[DeckListCard]] = Arbitrary(
    Gen.oneOf(
      CardList.empty[DeckListCard],
      CardList(DeckListCard("Tarmogoyf", 4)),
      CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 2)),
    )
  )

  implicit val arbitraryCardListInvenotryCard: Arbitrary[CardList[InventoryCard]] = Arbitrary(
    Gen.oneOf(
      CardList.empty[InventoryCard],
      CardList(InventoryCard("Tarmogoyf", 4, Edition("MM3"), NonFoil)),
      CardList(
        InventoryCard("Tarmogoyf", 4, Edition("MM3"), Foil),
        InventoryCard("Path to Exile", 2, Edition("CON"), NonFoil)
      ),
    )
  )
}
