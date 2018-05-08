package vdx.stockpile.cli

import java.io.File

import akka.actor.FSM
import cats.effect.IO
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cli.CoreSpec._
import vdx.stockpile.cli.UISpec.WorkerFinished

class CoreFSM(loadInventory: File => IO[InventoryLoaderResult], deckLoader: FileDeckLoader[DeckListCard])
    extends FSM[CoreSpec.State, CoreSpec.Data] {
  startWith(Uninitialized, Empty)

  private def appendExitHandler(pf: StateFunction): StateFunction =
    pf.orElse({ case Event(Exit, _) => stop() })

  private def load(file: File) = {
    val (logs, inventory) = loadInventory(file).unsafeRunSync().run
    context.parent ! UISpec.InventoryAvailable(logs)
    inventory
  }

  private def loadDecks(file: File) = {
    val (_, decks) = deckLoader.load(file).unsafeRunSync().run
    context.parent ! WorkerFinished(UISpec.DecksAreLoaded)
    decks
  }

  when(Uninitialized) {
    case Event(LoadInventory(file), Empty) =>
      goto(InventoryLoaded).using(StateData(Option(load(file)), List.empty))
  }

  when(InventoryLoaded)(appendExitHandler {
    case Event(LoadDecks(f: File), state: StateData) =>
      stay().using(StateData(state.inventory, List(loadDecks(f))))
    case Event(PrintInventory, ctx: StateData) =>
      context.parent ! UISpec.WorkerFinished(UISpec.InventoryResult(ctx.inventory.get))
      stay()
  })
}
