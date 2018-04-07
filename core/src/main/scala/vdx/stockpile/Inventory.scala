package vdx.stockpile

import cats.data.ValidatedNel

import scala.language.higherKinds

object Inventory {

  case class InventoryError(message: String)

  trait InventoryReaderAlg[F[_]] {
    def read: F[ValidatedNel[InventoryError, Inventory]]
  }

  trait InventoryWriterAlg[F[_]] {
    def write(inventory: Inventory): F[Either[InventoryError, Unit]]
  }
}
