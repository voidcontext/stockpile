package vdx.stockpile.cardlist

import vdx.stockpile.Card.{DeckListCard, InventoryCard}
import vdx.stockpile.instances.eq._

trait CardListInstances {
  implicit final def deckListCardListMonoid: CardListMonoid[DeckListCard] = new CardListMonoid[DeckListCard]()

  implicit final def inventoryCardCardListMonoid: CardListMonoid[InventoryCard] = new CardListMonoid[InventoryCard]()

  implicit final def cardListFoldable: CardListFoldable = new CardListFoldable
}
