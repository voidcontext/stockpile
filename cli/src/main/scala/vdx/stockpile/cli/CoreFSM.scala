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

  when(Uninitialized) {
    case Event(LoadInventory(file), Empty) =>
      val (logs, list) = loadInventory(file).unsafeRunSync().run
      goto(InventoryLoaded).using(Context(Option(list)))
  }

  when(InventoryLoaded)(appendExitHandler {
    case Event(PrintInventory, ctx: Context) =>
      context.parent ! UISpec.WorkerFinished(UISpec.InventoryResult(ctx.inventory.get))
      stay()
  })

  onTransition {
    case _ -> InventoryLoaded => context.parent ! UISpec.InventoryAvailable
  }
}
