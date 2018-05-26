package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorRef, ActorRefFactory, FSM}
import cats.Show
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.{Card, CardList, DeckList}
import vdx.stockpile.Inventory.InventoryLog
import vdx.stockpile.cli.Menu._
import vdx.stockpile.cli.console.Console
import vdx.stockpile.pricing.CardPrice

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
      case _ =>
        println("You've wandered too far... Exiting now.")
        self ! Terminate
        stay()
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
      def menuHandler: PartialFunction[MenuItem, Unit] = {
        case Menu.DistinctHaves =>
          core ! Core.DistinctHaves
        case Menu.DistinctMissing =>
          core ! Core.DistinctMissing
        case Menu.PriceDistinctMissing =>
          core ! Core.PriceDistinctMissing
      }

      console.menu(Menu.decks)(
        appendDefaultHandlers(
          menuHandler.andThen[State] { _ =>
            enterWorkingState(DeckLoadedScreen, data)
          },
          DeckLoadedScreen,
          data
        )
      )
  }

  def resultHandler: PartialFunction[Event, Unit] = {
    case Event(InventoryResult(inventory), _) =>
      inventory.toList.foreach(console.println)
    case Event(DistinctHaves(haves), _) =>
      haves.foreach { havesInDeck: HavesInDeck[_] =>
        console.println(havesInDeck.deckName)
        havesInDeck.haves.toList.foreach(console.println)
        console.println()
      }
    case Event(DistinctMissing(missing), _) =>
      missing.foreach { missing: MissingFromDeck[_] =>
        console.println(missing.deckName)
        missing.missing.toList.foreach(console.println)
        console.println()
      }
    case Event(DistinctMissingPrices(deckPrices), _) =>
      deckPrices.foreach { deckPrice: DeckPrice[_] =>
        console.println(deckPrice.deckName)
        deckPrice.prices.foreach(console.println)
        console.println()
      }
  }

  def notificationHandler: PartialFunction[Event, State] = {
    case Event(DecksAreLoaded, data: StateData) => goto(DeckLoadedScreen).using(data)
  }

  when(Working) {
    case e @ Event(_, data: StateData) => resultHandler.andThen(_ => goBack(data)).orElse(notificationHandler)(e)
  }

  whenUnhandled {
    case Event(Exit, data @ StateData(_)) =>
      println(" ---> Exit: back")
      goBack(data)
    case Event(Exit | Terminate, data: StateData) =>
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

  // Worker Results
  final case class HavesInDeck[A <: Card[A]](deckName: String, haves: CardList[A])
  final case class MissingFromDeck[A <: Card[A]](deckName: String, missing: CardList[A])
  final case class DeckPrice[A <: Card[A]](deckName: String, prices: List[CardPrice[A]])

  final case class InventoryResult(inventory: vdx.stockpile.Inventory)
  case object DecksAreLoaded
  final case class DistinctHaves[A <: Card[A]](haves: List[HavesInDeck[A]])
  final case class DistinctMissing[A <: Card[A]](missing: List[MissingFromDeck[A]])
  final case class DistinctMissingPrices[A <: Card[A]](deckPrices: List[DeckPrice[A]])

  // State
  sealed trait Screen extends State
  case object Uninitialized extends State
  case object Working extends State

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
  private case object Terminate extends Message

}
