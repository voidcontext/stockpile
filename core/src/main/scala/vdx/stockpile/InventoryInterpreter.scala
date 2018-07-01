package vdx.stockpile

import cats.data.Kleisli
import cats.syntax.applicative._
import cats.syntax.foldable._
import cats.syntax.monoid._
import cats.{Applicative, FlatMap, Foldable, Id}
import vdx.stockpile.Card.{DeckListCard, InventoryCard}
import vdx.stockpile.cardlist.CardList
import vdx.stockpile.instances.cardlist._
import vdx.stockpile.pricing.CardPrice

trait InventoryInterpreter
    extends InventoryAlgebra[Id, CardList, BuiltDeck, DeckListCard, InventoryCard, CardPrice[DeckListCard]] {

  override implicit def applicativeF: Applicative[Id] = cats.catsInstancesForId

  override implicit def flatMapF: FlatMap[Id] = cats.catsInstancesForId

  override implicit def cardListFoldable: Foldable[CardList] = vdx.stockpile.instances.cardlist.cardListFoldable

  override def cardsOwned(inventory: Inventory): Kleisli[Id, DeckList, DeckList] = Kleisli { deckList: DeckList =>
    deckList
      .foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
        val totalNumOfHaves = inventory.filter_(_.name == card.name).map(_.count).sum

        list.combine(CardList(card.withCount(Math.min(card.count, totalNumOfHaves))))
      }
      .pure[Id]
  }

  override def cardsToBuy(inventory: Inventory): Kleisli[Id, DeckList, DeckList] = Kleisli { deckList: DeckList =>
    deckList
      .foldLeft(CardList.empty[DeckListCard]) { (list, card) =>
        list.combine(
          CardList(
            inventory
              .filter_(_.name == card.name)
              .foldLeft(card) { (card, inventoryCard) =>
                card.withCount(card.count - inventoryCard.count)
              }
          )
        )
      }
      .pure[Id]

  }

  override def addCardToInventory(inventory: Inventory): Kleisli[EitherStringOr, InventoryCard, Inventory] = ???

  override def removeCardFromInventory(inventory: Inventory): Kleisli[EitherStringOr, InventoryCard, Inventory] = ???

  override def priceCard: Kleisli[Id, DeckListCard, CardPrice[DeckListCard]] = ???
}
