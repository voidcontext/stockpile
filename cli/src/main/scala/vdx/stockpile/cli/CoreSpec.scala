package vdx.stockpile.cli

import java.io.File

import akka.actor.ActorRef
import cats.effect.IO
import vdx.stockpile.Inventory
import vdx.stockpile.Inventory.InventoryLoaderResult

trait CoreSpec {
  sealed trait State
  sealed trait Data
  sealed trait Message

  case object Uninitialized extends State
  case object InventoryLoaded extends State

  case object Empty extends Data
  final case class Context(
    inventory: Option[Inventory]
  ) extends Data

  final case class LoadInventory(file: File) extends Message
  final case class Initialize(ref: ActorRef, inventoryLoader: File => IO[InventoryLoaderResult])
  case object PrintInventory extends Message
  case object Exit extends Message
}

object CoreSpec extends CoreSpec
