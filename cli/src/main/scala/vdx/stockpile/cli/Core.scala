package vdx.stockpile.cli

import java.io.File

import akka.actor.FSM
import cats.effect.IO
import vdx.stockpile.{Card, CardList, Deck, Inventory}
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.Deck.DeckLoaderResult
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cardlist.CardListOperations
import vdx.stockpile.cli.UI.WorkerFinished
import vdx.stockpile.instances.eq._

class Core(loadInventory: File => IO[InventoryLoaderResult], deckLoader: Core.FileDeckLoader[DeckListCard])
    extends FSM[Core.State, Core.Data]
    with CardListOperations {

  import Core._

  startWith(Uninitialized, Empty)

  private def appendExitHandler(pf: StateFunction): StateFunction =
    pf.orElse({ case Event(Exit, _) => stop() })

  private def load(file: File) = {
    val (logs, inventory) = loadInventory(file).unsafeRunSync().run
    context.parent ! UI.InventoryAvailable(logs)
    inventory
  }

  private def loadDecks(file: File) = {
    val (_, decks) = deckLoader.load(file).unsafeRunSync().run
    context.parent ! WorkerFinished(UI.DecksAreLoaded)
    decks
  }

  private def intersect[A <: Card[A], B <: Card[B]](list: CardList[A], other: CardList[B])(
    implicit i: Intersection[A, B, A]
  ) =
    i(list, other)

  when(Uninitialized) {
    case Event(LoadInventory(file), Empty) =>
      goto(InventoryLoaded).using(StateData(Option(load(file)), List.empty))
  }

  when(InventoryLoaded)(appendExitHandler {
    case Event(LoadDecks(f: File), state: StateData) =>
      stay().using(StateData(state.inventory, List(loadDecks(f))))
    case Event(DistinctHaves, state: StateData) =>
      context.parent ! UI.WorkerFinished(
        UI.DistinctHaves(
          state.decks.map {
            case deck: Deck[DeckListCard] => intersect(deck.toCardList, state.inventory.get)
          }
        )
      )
      stay()
    case Event(PrintInventory, ctx: StateData) =>
      context.parent ! UI.WorkerFinished(UI.InventoryResult(ctx.inventory.get))
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
    inventory: Option[Inventory],
    decks: List[Deck[_]]
  ) extends Data

  // Messages
  final case class LoadInventory(file: File) extends Message
  final case class LoadDecks(file: File) extends Message
  case object DistinctHaves extends Message
  case object PrintInventory extends Message
  case object Exit extends Message

  private[cli] trait FileDeckLoader[A <: Card[A]] {
    def load(file: File): IO[DeckLoaderResult[A]]
  }
}
