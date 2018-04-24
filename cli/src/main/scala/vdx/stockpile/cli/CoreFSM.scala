package vdx.stockpile.cli

import akka.actor.FSM
import vdx.stockpile.cli.CoreSpec._

class CoreFSM extends FSM[CoreSpec.State, CoreSpec.Data] {
  startWith(Uninitialized, Empty)

  private def appendExitHandler(pf: StateFunction): StateFunction =
    pf.orElse({ case Event(Exit, _) => stop() })

  when(Uninitialized) {
    case Event(Initialize(ref, inventoryLoader), Empty) =>
      goto(Initialized).using(Context(ref, inventoryLoader, None))
  }

  when(Initialized) {
    case Event(LoadInventory(file), ctx: Context) =>
      val (logs, list) = ctx.inventoryLoader(file).unsafeRunSync().run
      goto(InventoryLoaded).using(ctx.copy(inventory = Option(list)))
  }

  when(InventoryLoaded)(appendExitHandler {
    case Event(PrintInventory, ctx: Context) =>
      ctx.ref ! UISpec.WorkerFinished(UISpec.InventoryResult(ctx.inventory.get))
      stay()
  })

  onTransition {
    case Initialized -> InventoryLoaded =>
      stateData match {
        case ctx: Context =>
          ctx.ref ! UISpec.InventoryAvailable
      }
  }
}
