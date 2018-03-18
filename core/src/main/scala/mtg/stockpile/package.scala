package mtg

package object stockpile {
  sealed trait FoilState

  final case object Foil extends FoilState
  final case object NonFoil extends FoilState

  final case class Edition(code: String)

  sealed trait Card {
    def name: String

    def count: Int
  }

  sealed trait ConcreteCard extends Card {
    def edition: Edition
  }

  /**
    * Represent a card in a decklist
    *
    * @param name Name of the card
    * @param count Amount
    */
  final case class DeckListCard(name: String, count: Int) extends Card {
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
  final case class InventoryCard(name: String, count: Int, edition: Edition, foil: FoilState) extends ConcreteCard

  /**
    * Represents a wanted card. Edition and foil state is optional as we don't always want to specify this.
    *
    * @param name
    * @param count
    * @param edition
    * @param foil
    */
  final case class WantsListCard(name: String, count: Int, edition: Option[Edition], foil: Option[FoilState])
      extends Card

  /**
    * Inventory is someone's whole collection
    *
    * @param cards List of InvendtoryCards
    */
  final case class Inventory(cards: Set[InventoryCard])

  /**
    * Trade list is a specific type of inventory, it contains cards that can be traded away
    * @param cards
    */
  final case class TradeList(cards: Set[InventoryCard])

  /**
    * The list of wanted cards
    * @param cards
    */
  final case class WantsList(cards: Set[WantsListCard])

  /**
    * Representation of a deck. Cards in deck lists don't have an edition
    *
    * @param mainBoard
    * @param sideBoard
    * @param maybeBoard
    */
  final case class DeckList(
    mainBoard: Set[DeckListCard],
    sideBoard: Set[DeckListCard],
    maybeBoard: Set[DeckListCard],
  ) extends

  /**
    * Represents a deck built from an inventory
    *
    * @param mainBoard
    * @param sideBoard
    * @param maybeBoard
    */
  final case class BuiltDeck(
    mainBoard: Set[InventoryCard],
    sideBoard: Set[InventoryCard],
    maybeBoard: Set[InventoryCard],
  )

}
