package vdx.stockpile.cli

import akka.actor.FSM
import vdx.stockpile.cli.UISpec._

class UIFSM extends FSM[State, Data] {
  startWith(Uninitialized, Empty)

  private def defaultHandlers(currentScreen: Screen, context: Context): PartialFunction[MenuItem, State] = {
    case a: Action =>
      context.actionRunner(a)
      enterWorkingState(currentScreen, context)
    case Menu.Nop =>
      stay()
    case Menu.Quit =>
      self ! Exit
      stay()
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
    case Context(_, _, head :: tail) => goto(head).using(data.copy(screenStack = tail))
  }

  when(Uninitialized) {
    case Event(Initialize(actionRunner, console), Empty) =>
      goto(Initialized).using(Context(console, actionRunner, List.empty))
  }

  when(Initialized) {
    case Event(InventoryAvailable, data) => goto(InventoryOnlyScreen).using(data)
  }

  when(InventoryOnlyScreen) {
    case Event(DrawMenu, data: Context) => {
      data.console.menu(Menu.main)(
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
      data.console.menu(Menu.export)(defaultHandlers(InventoryExportScreen, data))
  }

  when(Working) {
    case Event(WorkerFinished(r: InventoryResult), data @ Context(_, _, head :: tail)) =>
      r.inventory.toList.foreach(println)
      goBack(data)
  }

  whenUnhandled {
    case Event(Exit, data @ Context(_, _, head :: tail)) =>
      println(" ---> Exit: back")
      goBack(data)
    case Event(Exit, data: Context) =>
      println(data)
      println(" ---> Exit: exit")
//      data.backendRef ! CoreSpec.Exit
      context.system.terminate()
      stop()
  }

  onTransition {
    case (_, _: Screen) => self ! DrawMenu
  }
}
