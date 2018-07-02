package vdx

import cats.arrow.FunctionK
import cats.data.Kleisli
import cats.instances.either._
import cats.instances.list._
import cats.syntax.traverse._
import cats.{Applicative, FlatMap, Foldable, Id}
import vdx.stockpile.Card.{DeckListCard, InventoryCard, WantsListCard}
import vdx.stockpile.cardlist.CardList

package object stockpile {

  type Inventory = CardList[InventoryCard]
  type TradeList = CardList[InventoryCard]
  type WantsList = CardList[WantsListCard]
  type DeckList = CardList[DeckListCard]

  type DeckListDeck = Deck[DeckListCard]
  type BuiltDeck = Deck[InventoryCard]

  trait StockpileAlgebra {
    type EitherStringOr[A] = Either[String, A]
  }

  //###################################
  //# Card / Deck management
  //###################################
  trait DeckAlgebra[F[_], CardList[_], DeckListCard, InventoryCard, DeckListCardPrice] extends StockpileAlgebra {

    type Inventory = CardList[InventoryCard]
    type DeckList = CardList[DeckListCard]

    type ListF[A] = List[F[A]]
    type FList[A] = F[List[A]]

    implicit def cardListFoldable: Foldable[CardList]

    implicit def applicativeF: Applicative[F]

    implicit def flatMapF: FlatMap[F]

    def cardList2List[A](cardList: CardList[A]): List[A] = cardListFoldable.toList[A](cardList)

    lazy val idToF: FunctionK[Id, F] = new FunctionK[Id, F] {
      override def apply[A](fa: Id[A]): F[A] = applicativeF.pure(fa)
    }

    def cardsOwned(inventory: Inventory): Kleisli[Id, DeckList, DeckList]

    def cardsToBuy(inventory: Inventory): Kleisli[Id, DeckList, DeckList]

    def priceCard: Kleisli[F, DeckListCard, DeckListCardPrice]

    def priceCards: Kleisli[F, List[DeckListCard], List[DeckListCardPrice]] = Kleisli { cards: List[DeckListCard] =>
      cards.map(priceCard.run).traverse[F, DeckListCardPrice](l => l)(applicativeF)
    }

    def priceCards(fa: Kleisli[Id, DeckList, DeckList]): Kleisli[F, DeckList, List[DeckListCardPrice]] = {
      fa.andThen(cardList2List[DeckListCard] _)
        .mapK(idToF)
        .andThen(priceCards)
    }

    def priceCardsToBuy(inventory: Inventory): Kleisli[F, DeckList, List[DeckListCardPrice]] =
      priceCards(cardsToBuy(inventory))
  }

  //###################################
  //# Inventory management
  //###################################

  trait InventoryAlgebra[CardList[_], InventoryCard] extends StockpileAlgebra {

    type Inventory = CardList[InventoryCard]
    type TradeResult = (Inventory, Inventory)

    def addCardToInventory(inventory: Inventory): Kleisli[EitherStringOr, InventoryCard, Inventory]

    def removeCardFromInventory(inventory: Inventory): Kleisli[EitherStringOr, InventoryCard, Inventory]

    def tradeCard(from: Inventory, to: Inventory): Kleisli[EitherStringOr, InventoryCard, TradeResult] = {
      for {
        src <- removeCardFromInventory(from)
        dest <- addCardToInventory(to)
      } yield (src, dest)
    }
  }

}
