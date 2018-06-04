package vdx.stockpile.instances

import cats.Eq
import cats.instances.option._
import cats.instances.string._
import cats.syntax.eq._
import vdx.stockpile.Card
import vdx.stockpile.Card._
import vdx.stockpile.cardlist.{CardList, CardListInstances}

trait EqInstances extends CardListInstances {
  implicit val editionEq: Eq[Edition] = (x: Edition, y: Edition) => x.code === y.code

  implicit val foilStateEq: Eq[FoilState] = (x: FoilState, y: FoilState) => x == y

  implicit val deckListCardEq: Eq[DeckListCard] = (x: DeckListCard, y: DeckListCard) => x.name === y.name

  implicit val inventoryCardEq: Eq[InventoryCard] = (x: InventoryCard, y: InventoryCard) =>
    x.name === y.name && x.edition === y.edition && x.foil == y.foil

  implicit val wantsListCardEq: Eq[WantsListCard] = (x: WantsListCard, y: WantsListCard) =>
    x.name === y.name && x.edition === y.edition && x.foil === y.foil

}
