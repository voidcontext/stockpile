package vdx

import vdx.stockpile.Card.{DeckListCard, InventoryCard, WantsListCard}

package object stockpile {

  type Inventory = CardList[InventoryCard]
  type TradeList = CardList[InventoryCard]
  type WantsList = CardList[WantsListCard]

  type DeckList = Deck[DeckListCard]
  type BuiltDeck = Deck[InventoryCard]
}
