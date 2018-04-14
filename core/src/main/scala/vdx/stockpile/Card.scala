package vdx.stockpile

sealed trait Card[A <: Card[A]] { self: A =>
  def name: String

  def count: Int

  def withCount(count: Int): A
}

object Card {
  sealed trait FoilState

  final case object Foil extends FoilState
  final case object NonFoil extends FoilState

  final case class Edition(code: String)

  /**
    * Represent a card in a decklist
    *
    * @param name Name of the card
    * @param count Amount
    */
  final case class DeckListCard(name: String, count: Int) extends Card[DeckListCard] {
    def withCount(count: Int): DeckListCard = copy(count = count)
  }

  /**
    * Represents a concrete physical card. These cards always have a well defined edition, foil state and count.
    *
    * @param name Name of the card
    * @param count Amount
    * @param edition The edition where the card was printed
    * @param foil Foil or not
    */
  final case class InventoryCard(name: String, count: Int, edition: Edition, foil: FoilState)
      extends Card[InventoryCard] {
    def withCount(count: Int): InventoryCard = copy(count = count)
  }

  /**
    * Represents a wanted card. Edition and foil state is optional as we don't always want to specify this.
    *
    * @param name
    * @param count
    * @param edition
    * @param foil
    */
  final case class WantsListCard(name: String, count: Int, edition: Option[Edition], foil: Option[FoilState])
      extends Card[WantsListCard] {
    def withCount(count: Int): WantsListCard = copy(count = count)
  }
}
