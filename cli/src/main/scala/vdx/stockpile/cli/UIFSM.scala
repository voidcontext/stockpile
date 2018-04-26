package vdx.stockpile.cli

import akka.actor.{ActorRef, ActorRefFactory, FSM, Props}
import vdx.stockpile.cli.UISpec._
import vdx.stockpile.cli.console.Console

class UIFSM(childFactory: ActorRefFactory => ActorRef, console: Console) extends FSM[State, Data] {
  val core = childFactory(context)

  override def preStart(): Unit = {
    super.preStart()

    // TODO: inject file somehow
    core ! CoreSpec.LoadInventory(loadLastInventoryFromDownloads())
  }

  startWith(Uninitialized, Empty)

  private def defaultHandlers(currentScreen: Screen, context: Context): PartialFunction[MenuItem, State] = {
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
    case Menu.InventoryExportTerminal => core ! CoreSpec.PrintInventory
  }

  private def appendDefaultHandlers(
    handler: PartialFunction[MenuItem, State],
    currentScreen: Screen,
    context: Context
  ) =
    handler.orElse(defaultHandlers(currentScreen, context))

  private def enterSubScreen(screen: Screen, current: Screen, data: Context) =
    goto(screen).using(data.copy(screenStack = current :: data.screenStack))

  private def enterWorkingState(current: Screen, data: Context) =
    goto(Working).using(data.copy(screenStack = current :: data.screenStack))

  private def goBack(data: Context) = data match {
    case Context(head :: tail) => goto(head).using(data.copy(screenStack = tail))
  }

  when(Uninitialized) {
    case Event(InventoryAvailable, Empty) =>
      goto(InventoryOnlyScreen).using(Context(List.empty))
  }

  when(InventoryOnlyScreen) {
    case Event(DrawMenu, data: Context) => {
      console.menu(Menu.main)(
        appendDefaultHandlers(
          {
            case Menu.InventoryExport =>
              enterSubScreen(InventoryExportScreen, InventoryOnlyScreen, data)
          },
          InventoryOnlyScreen,
          data
        )
      )
    }
  }

  when(InventoryExportScreen) {
    case Event(DrawMenu, data: Context) =>
      console.menu(Menu.export)(defaultHandlers(InventoryExportScreen, data))
  }

  when(Working) {
    case Event(WorkerFinished(r: InventoryResult), data @ Context(head :: tail)) =>
      r.inventory.toList.foreach(console.println)
      goBack(data)
  }

  whenUnhandled {
    case Event(Exit, data @ Context(head :: tail)) =>
      println(" ---> Exit: back")
      goBack(data)
    case Event(Exit, data: Context) =>
      println(data)
      println(" ---> Exit: exit")
      context.system.terminate()
      stop()
  }

  onTransition {
    case (_, _: Screen) => self ! DrawMenu
  }
}
