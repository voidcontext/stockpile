package vdx.stockpile

import java.io.File

import cats.data.Writer

object Inventory {

  type InventoryLoaderResult = Writer[Vector[InventoryLog], Inventory]
  type InventoryWriterResult = Writer[Vector[InventoryLog], File]

  sealed trait InventoryLog {
    def message: String
  }

  final case class InventoryError(message: String) extends InventoryLog

  trait InventoryLoaderAlg[F[_]] {
    def load: F[InventoryLoaderResult]
  }

  trait InventoryWriterAlg[F[_]] {
    def write(inventory: Inventory): F[InventoryWriterResult]
  }
}
