package vdx.stockpile

import cats.effect.IO

trait InventoryLoader {
  def loadInventory: IO[Inventory]
}
