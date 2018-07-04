package vdx.stockpile.cli

import cats.Eval
import cats.data.State
import javafx.stage.Screen
import vdx.stockpile.Inventory
import vdx.stockpile.cardlist.CardList

import scala.annotation.tailrec

object StockpileApp extends App {

  sealed trait State
  final case class Uninitialized() extends State
  final case class MainScreen(inventory: Inventory) extends State
  final case class ExportScreen(inventory: Inventory) extends State
  final case class Terminated() extends State

  sealed trait Action
  final case class Initialize() extends Action
  final case class Export() extends Action
  final case class PrintInventory() extends Action
  final case class PreviousScreen() extends Action
  final case class Exit() extends Action

  sealed trait StateAction
  final case class Stay() extends StateAction
  final case class Next(state: State) extends StateAction
  final case class Back() extends StateAction
  final case class Reset(initial: State) extends StateAction
  final case class Terminate() extends StateAction

  trait CLIAlgebra {
    def initialize(): StateAction

    def printInventory(inventory: Inventory): StateAction
  }

  object TestInterpreter extends CLIAlgebra {
    override def initialize(): StateAction = Next(MainScreen(CardList.empty))

    override def printInventory(inventory: Inventory): StateAction = {
      println(inventory)
      Stay()
    }
  }

  class CLIApp(algebra: CLIAlgebra) {
    import algebra._

//    var mainScreenActions = List(PrintInventory(), Exit())
    var mainScreenActions = List(Export(), Exit())
//    var exportScreenActions = List(PrintInventory(), Exit())
    var exportScreenActions = List(PrintInventory(), PreviousScreen(), Exit())
    private def action(state: State): Action = state match {
      case Uninitialized() => Initialize()
      case MainScreen(_) =>
        mainScreenActions match {
          case head :: tail =>
            mainScreenActions = tail
            head
        }
      case ExportScreen(_) =>
        exportScreenActions match {
          case head :: tail =>
            exportScreenActions = tail
            head
        }
    }

    private def transformState: PartialFunction[(State, Action), StateAction] = {
      case (Uninitialized(), Initialize())             => initialize()
      case (MainScreen(inventory), Export())           => Next(ExportScreen(inventory))
      case (ExportScreen(inventory), PrintInventory()) => printInventory(inventory)
      case (MainScreen(_), Exit())                     => Terminate()
      case (_, PreviousScreen())                       => Back()
      case (state, action) => {
        println(s"Illegal transformation from '${state.getClass}' by '${action.getClass}'")
        Terminate()
      }
    }

    def loop(stack: List[State]): Eval[State] = {
      transformState(stack.head, action(stack.head)) match {
        case Stay()          => Eval.defer(loop(stack))
        case Next(newState)  => Eval.defer(loop(newState :: stack))
        case Back()          => Eval.defer(loop(stack.tail))
        case Reset(newState) => Eval.defer(loop(newState :: Nil))
        case Terminate()     => Eval.now(Terminated())
      }
    }

    def run(): State = {
      loop(Uninitialized() :: Nil).value
    }
  }

  println(new CLIApp(TestInterpreter).run())
}
