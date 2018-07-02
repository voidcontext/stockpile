package vdx.stockpile.cli

import java.io.File

import akka.actor.FSM
import cats.data.Writer
import cats.instances.list._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.{Id, Monad}
import vdx.stockpile.Card.{DeckListCard, InventoryCard}
import vdx.stockpile.Deck.DeckLog
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile._
import vdx.stockpile.cardlist.CardList
import vdx.stockpile.cli.Extractor.syntax._
import vdx.stockpile.instances.eq._
import vdx.stockpile.pricing.CardPrice

class Core[F[_]: Monad: Extractor, G[_]: Monad: Extractor]()(implicit cCtx: Core.CoreContext[F, G])
    extends FSM[Core.State, Core.Data] {

  import Core._

  startWith(Uninitialized, Empty)

  private def appendExitHandler(pf: StateFunction): StateFunction =
    pf.orElse({ case Event(Exit, _) => stop() })

  private def load(file: File) = {
    val (logs, inventory) = cCtx.inventoryLoader(file).extract.run
    context.parent ! UI.InventoryAvailable(logs)
    inventory
  }

  private def loadDecks(file: File) = {
    val (_, decks) = cCtx.deckLoader.load(file).extract.run
    context.parent ! UI.DecksAreLoaded
    decks
  }

  when(Uninitialized) {
    case Event(LoadInventory(file), Empty) =>
      goto(InventoryLoaded).using(StateData(Option(load(file)), List.empty))
  }

  when(InventoryLoaded)(appendExitHandler {
    case Event(LoadDecks(f: File), state: StateData) =>
      stay().using(StateData(state.inventory, loadDecks(f)))
    case Event(DistinctHaves, state: StateData) =>
      context.parent ! UI.DistinctHaves(
        state.deckLists.map { deck =>
          UI.HavesInDeck(
            deck.name,
            cCtx.deckAlgebra.cardsOwned(state.inventory.get)(deck.toCardList)
          )
        }
      )

      stay()
    case Event(DistinctMissing, state: StateData) => {
      def missingCards(deck: Deck[DeckListCard]): Id[UI.MissingFromDeck[DeckListCard]] = {
        cCtx.deckAlgebra
          .cardsToBuy(state.inventory.get)(deck.toCardList)
          .map { cardsToBuy: DeckList =>
            UI.MissingFromDeck(
              deck.name,
              cardsToBuy
            )
          }
      }

      context.parent ! UI.DistinctMissing[DeckListCard](
        state.deckLists
          .map(missingCards)
          .traverse(identity)
      )
      stay()
    }
    case Event(PrintInventory, ctx: StateData) =>
      context.parent ! UI.InventoryResult(ctx.inventory.get)
      stay()
  })
}

object Core {
  sealed trait State
  sealed trait Data
  sealed trait Message

  // States
  case object Uninitialized extends State
  case object InventoryLoaded extends State
  case object DecksLoaded extends State

  // State Data
  case object Empty extends Data
  final case class StateData(
    inventory: Option[CardList[InventoryCard]],
    deckLists: List[Deck[DeckListCard]]
  ) extends Data

  // Messages
  final case class LoadInventory(file: File) extends Message
  final case class LoadDecks(file: File) extends Message
  case object DistinctHaves extends Message
  case object DistinctMissing extends Message
  case object PriceDistinctMissing extends Message
  case object PrintInventory extends Message
  case object Exit extends Message

  case class CoreContext[F[_], G[_]](
    inventoryLoader: File => G[InventoryLoaderResult],
    deckLoader: Core.FileDeckLoader[DeckListCard, G],
    deckAlgebra: DeckAlgebra[F, CardList, DeckListCard, InventoryCard, CardPrice[DeckListCard]]
  )

  type FileDeckLoaderResult[A <: Card[A]] = Writer[Vector[DeckLog], List[Deck[A]]]

  private[cli] trait FileDeckLoader[A <: Card[A], F[_]] {
    def load(file: File): F[FileDeckLoaderResult[A]]
  }

}
