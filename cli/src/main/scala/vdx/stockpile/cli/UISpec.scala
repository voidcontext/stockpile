package vdx.stockpile.cli

import vdx.stockpile.cli.console.Console

trait UISpec {
  sealed trait State
  sealed trait Data
  sealed trait Message
  sealed trait WorkerResult

  final case class InventoryResult(inventory: vdx.stockpile.Inventory) extends WorkerResult

  // State
  sealed trait Screen extends State
  case object Uninitialized extends State
  case object Working extends State
  final case class WorkerFinished(result: WorkerResult) extends State

  // Screens
  case object InventoryOnlyScreen extends Screen
  case object InventoryExportScreen extends Screen

  // Data
  case object Empty extends Data
  final case class StateData(
    screenStack: List[Screen]
  ) extends Data

  // Messages
  case object CoreIsReady extends Message
  case object InventoryAvailable extends Message
  case object DrawMenu extends Message
  case object Exit extends Message
}

object UISpec extends UISpec
