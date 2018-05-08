package vdx.stockpile.cli

import java.io.File

import cats.effect.IO
import vdx.stockpile.Deck.DeckLoaderResult
import vdx.stockpile.{Card, Deck, Inventory}

trait CoreSpec {
  sealed trait State
  sealed trait Data
  sealed trait Message

  case object Uninitialized extends State
  case object InventoryLoaded extends State
  case object DecksLoaded extends State

  case object Empty extends Data
  final case class StateData(
    inventory: Option[Inventory],
    decks: List[Deck[_]]
  ) extends Data

  final case class LoadInventory(file: File) extends Message
  final case class LoadDecks(file: File) extends Message
  case object PrintInventory extends Message
  case object Exit extends Message

  private[cli] trait FileDeckLoader[A <: Card[A]] {
    def load(file: File): IO[DeckLoaderResult[A]]
  }
}

object CoreSpec extends CoreSpec
