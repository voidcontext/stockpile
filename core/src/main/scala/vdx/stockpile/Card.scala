package vdx.stockpile

sealed trait Card[A <: Card[A]] { self: A =>
  def name: String

  def count: Int

  def withCount(count: Int): A
}

object Card {
  sealed trait FoilState

  case object Foil extends FoilState
  case object NonFoil extends FoilState
  case object PreReleaseFoil extends FoilState

  final case class Edition(code: String)

  /**
   * Represent a card in a decklist
   */
  final case class DeckListCard(name: String, count: Int) extends Card[DeckListCard] {
    def withCount(count: Int): DeckListCard = copy(count = count)
  }

  /**
   * Represents a concrete physical card. These cards always have a well defined edition, foil state and count.
   */
  final case class InventoryCard(name: String, count: Int, edition: Edition, foil: FoilState)
      extends Card[InventoryCard] {
    def withCount(count: Int): InventoryCard = copy(count = count)
  }

  /**
   * Represents a wanted card. Edition and foil state is optional as we don't always want to specify this.
   */
  final case class WantsListCard(name: String, count: Int, edition: Option[Edition], foil: Option[FoilState])
      extends Card[WantsListCard] {
    def withCount(count: Int): WantsListCard = copy(count = count)
  }
}
