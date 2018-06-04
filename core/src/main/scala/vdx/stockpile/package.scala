package vdx

import vdx.stockpile.Card.{DeckListCard, InventoryCard, WantsListCard}
import vdx.stockpile.cardlist.CardList

package object stockpile {

  type Inventory = CardList[InventoryCard]
  type TradeList = CardList[InventoryCard]
  type WantsList = CardList[WantsListCard]
  type DeckList = CardList[DeckListCard]

  type DeckListDeck = Deck[DeckListCard]
  type BuiltDeck = Deck[InventoryCard]

  trait InventoryAlgebra[Inventory, DeckList, BuiltDeck] {
    def cardsOwned(inventory: Inventory, deckList: DeckList): DeckList
    def cardsToBuy(inventory: Inventory, deckList: DeckList): DeckList
  }
}
