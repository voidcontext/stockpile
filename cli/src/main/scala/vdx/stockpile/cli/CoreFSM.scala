package vdx.stockpile.cli

import java.io.File

import akka.actor.FSM
import cats.effect.IO
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cli.CoreSpec._

class CoreFSM(loadInventory: File => IO[InventoryLoaderResult]) extends FSM[CoreSpec.State, CoreSpec.Data] {
  startWith(Uninitialized, Empty)

  private def appendExitHandler(pf: StateFunction): StateFunction =
    pf.orElse({ case Event(Exit, _) => stop() })

  private def load(file: File) = {
    val (logs, inventory) = loadInventory(file).unsafeRunSync().run
    context.parent ! UISpec.InventoryAvailable(logs)
    inventory
  }

  when(Uninitialized) {
    case Event(LoadInventory(file), Empty) =>
      goto(InventoryLoaded).using(StateData(Option(load(file))))
  }

  when(InventoryLoaded)(appendExitHandler {
    case Event(PrintInventory, ctx: StateData) =>
      context.parent ! UISpec.WorkerFinished(UISpec.InventoryResult(ctx.inventory.get))
      stay()
  })
}
