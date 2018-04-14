package vdx.stockpile

import cats.data.Writer

object Inventory {

  sealed trait InventoryLoaderLog {
    def message: String
  }

  final case class InventoryError(message: String) extends InventoryLoaderLog

  trait InventoryLoaderAlg[F[_]] {
    def load: F[Writer[Vector[InventoryLoaderLog], Inventory]]
  }

  trait InventoryWriterAlg[F[_]] {
    def write(inventory: Inventory): F[Either[InventoryError, Unit]]
  }
}
