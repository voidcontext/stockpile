package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorRef, ActorRefFactory, FSM}
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.{Card, CardList}
import vdx.stockpile.Inventory.InventoryLog
import vdx.stockpile.cli.Menu._
import vdx.stockpile.cli.console.Console

class UI(childFactory: ActorRefFactory => ActorRef, console: Console) extends FSM[UI.State, UI.Data] {
  import UI._

  val core = childFactory(context)

  override def preStart(): Unit = {
    super.preStart()

    // TODO: inject file somehow
    core ! Core.LoadInventory(loadLastInventoryFromDownloads())
  }

  startWith(Uninitialized, Empty)

  private def defaultHandlers(currentScreen: Screen, context: StateData): PartialFunction[MenuItem, State] = {
    case a: Action =>
      runAction(a)
      enterWorkingState(currentScreen, context)
    case Menu.Nop =>
      stay()
    case Menu.Quit =>
      self ! Exit
      stay()
  }
  private def runAction: PartialFunction[Action, Unit] = {
    case Menu.InventoryExportTerminal => core ! Core.PrintInventory
  }

  private def appendDefaultHandlers(
    handler: PartialFunction[MenuItem, State],
    currentScreen: Screen,
    context: StateData
  ) =
    handler.orElse(defaultHandlers(currentScreen, context))

  private def enterSubScreen(screen: Screen, current: Screen, data: StateData) =
    goto(screen).using(data.copy(screenStack = current :: data.screenStack))

  private def enterWorkingState(current: Screen, data: StateData) =
    goto(Working).using(data.copy(screenStack = current :: data.screenStack))

  private def goBack(data: StateData) =
    data match {
      case StateData(head :: tail) => goto(head).using(data.copy(screenStack = tail))
    }

  when(Uninitialized) {
    case Event(InventoryAvailable(logs), Empty) =>
      logs.foreach(log => console.println(log.message))
      goto(InventoryOnlyScreen).using(StateData(List.empty))
  }

  when(InventoryOnlyScreen) {
    case Event(DrawMenu, data: StateData) => {
      console.menu(Menu.main)(
        appendDefaultHandlers(
          {
            case Menu.InventoryExport =>
              enterSubScreen(InventoryExportScreen, InventoryOnlyScreen, data)
            case Menu.LoadDecksFromDir =>
              val dir = console.readLine("Deck dir: ")
              core ! Core.LoadDecks(new File(dir))
              enterWorkingState(InventoryOnlyScreen, data)
          },
          InventoryOnlyScreen,
          data
        )
      )
    }
  }

  when(InventoryExportScreen) {
    case Event(DrawMenu, data: StateData) =>
      console.menu(Menu.export)(defaultHandlers(InventoryExportScreen, data))
  }

  when(DeckLoadedScreen) {
    case Event(DrawMenu, data: StateData) =>
      console.menu(Menu.decks)(
        appendDefaultHandlers(
          {
            case Menu.DistinctHaves =>
              core ! Core.DistinctHaves
              enterWorkingState(DeckLoadedScreen, data)
            case Menu.DistinctMissing =>
              core ! Core.DistinctMissing
              enterWorkingState(DeckLoadedScreen, data)
          },
          DeckLoadedScreen,
          data
        )
      )
  }

  when(Working) {
    case Event(WorkerFinished(r: InventoryResult), data: StateData) =>
      r.inventory.toList.foreach(console.println)
      goBack(data)
    case Event(WorkerFinished(DecksAreLoaded), data: StateData) =>
      goto(DeckLoadedScreen).using(data)
    case Event(WorkerFinished(ds: DistinctHaves[DeckListCard]), data: StateData) =>
      ds.haves.foreach { have =>
        console.println(have.deckName)
        have.haves.toList.foreach(console.println)
        console.println()
      }
      goBack(data)
    case Event(WorkerFinished(dm: DistinctMissing[DeckListCard]), data: StateData) =>
      dm.missing.foreach { missing =>
        console.println(missing.deckName)
        missing.missing.toList.foreach(console.println)
        console.println()
      }
      goBack(data)
  }

  whenUnhandled {
    case Event(Exit, data @ StateData(head :: tail)) =>
      println(" ---> Exit: back")
      goBack(data)
    case Event(Exit, data: StateData) =>
      println(data)
      println(" ---> Exit: exit")
      context.system.terminate()
      stop()
  }

  onTransition {
    case (_, _: Screen) => self ! DrawMenu
  }
}

object UI {
  sealed trait State
  sealed trait Data
  sealed trait Message
  sealed trait WorkerResult

  // Worker Results
  final case class InventoryResult(inventory: vdx.stockpile.Inventory) extends WorkerResult
  case object DecksAreLoaded extends WorkerResult
  final case class HavesInDeck[A <: Card[A]](deckName: String, haves: CardList[A])
  final case class MissingFromDeck[A <: Card[A]](deckName: String, missing: CardList[A])
  final case class DistinctHaves[A <: Card[A]](haves: List[HavesInDeck[A]]) extends WorkerResult
  final case class DistinctMissing[A <: Card[A]](missing: List[MissingFromDeck[A]]) extends WorkerResult

  // State
  sealed trait Screen extends State
  case object Uninitialized extends State
  case object Working extends State
  final case class WorkerFinished(result: WorkerResult) extends State

  // Screens
  case object InventoryOnlyScreen extends Screen
  case object InventoryExportScreen extends Screen
  case object DeckLoadedScreen extends Screen

  // Data
  case object Empty extends Data
  final case class StateData(
    screenStack: List[Screen]
  ) extends Data

  // Messages
  case object CoreIsReady extends Message
  final case class InventoryAvailable(logs: Seq[InventoryLog]) extends Message
  case object DrawMenu extends Message
  case object Exit extends Message

}
