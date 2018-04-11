package vdx.stockpile

import cats.data.Writer

object Inventory {

  sealed trait InventoryReaderLog extends Error

  final case class InventoryError(message: String) extends InventoryReaderLog

  trait InventoryReaderAlg[F[_]] {
    def read: F[Writer[Vector[InventoryReaderLog], Inventory]]
  }

  trait InventoryWriterAlg[F[_]] {
    def write(inventory: Inventory): F[Either[InventoryError, Unit]]
  }
}
