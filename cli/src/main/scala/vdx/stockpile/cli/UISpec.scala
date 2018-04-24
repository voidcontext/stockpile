package vdx.stockpile.cli

import akka.actor.ActorRef
import vdx.stockpile.cli.console.Console

trait UISpec {
  sealed trait State
  sealed trait Data
  sealed trait Message
  sealed trait WorkerResult

  final case class InventoryResult(inventory: vdx.stockpile.Inventory) extends WorkerResult

  // State
  trait Screen extends State
  case object Uninitialized extends State
  case object Initialized extends State
  case object InventoryOnlyScreen extends Screen
  case object InventoryExportScreen extends Screen
  case object Working extends State
  case class WorkerFinished(result: WorkerResult) extends State

  // Data
  final case object Empty extends Data
  case class Context(
    console: Console,
    actionRunner: PartialFunction[MenuItem, Unit],
    screenStack: List[Screen]
  ) extends Data

  // Messages
  case class Initialize(actionRunner: PartialFunction[MenuItem, Unit], console: Console) extends Message
  case object InventoryAvailable extends Message
  case object DrawMenu extends Message
  case object Exit extends Message
}

object UISpec extends UISpec
