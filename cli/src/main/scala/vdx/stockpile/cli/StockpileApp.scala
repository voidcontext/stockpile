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
  final case class Terminated() extends State

  sealed trait Action
  final case class Initialize(inventory: Inventory) extends Action
  final case class PrintInventory() extends Action
  final case class Exit() extends Action

  def transform: PartialFunction[(State, Action), State] = {
    case (Uninitialized(), Initialize(inventory)) =>
      MainScreen(inventory)
    case (MainScreen(inventory), PrintInventory()) =>
      println(inventory)
      MainScreen(inventory)
    case (state, action) =>
      Terminated()
  }

  var mainScreenActions = List(PrintInventory(), Exit())

  def action(state: State): Action = state match {
    case Uninitialized() => Initialize(CardList.empty)
    case MainScreen(_) =>
      mainScreenActions match {
        case head :: tail =>
          mainScreenActions = tail
          head
      }
  }

  def loop(state: State): Eval[State] = {
    transform(state, action(state)) match {
      case newState @ Terminated() => Eval.now(newState)
      case nextState               => Eval.defer(loop(nextState))
    }
  }

  println(loop(Uninitialized()).value)
}
