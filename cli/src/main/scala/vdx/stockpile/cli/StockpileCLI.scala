package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import cats.effect.IO
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cli.console.Terminal
import vdx.stockpile.inventory.format.deckbox.{IOCsvParserInterpreter, InventoryReaderThroughParserAndCardDBInterpreter}
import vdx.stockpile.mgjson.MtgJsonDBInterpreter

object StockpileCLI extends App {

  val system = ActorSystem()

  def loadInventoryFromFile: File => IO[InventoryLoaderResult] = (csvFile: File) => {
    new InventoryReaderThroughParserAndCardDBInterpreter[IO](
      new IOCsvParserInterpreter(csvFile),
      new MtgJsonDBInterpreter {}
    ).load
  }

  val ui = system.actorOf(
    Props(
      classOf[UIFSM],
      (system: ActorRefFactory) => system.actorOf(Props(classOf[CoreFSM], loadInventoryFromFile)),
      new Terminal
    ),
    "ui"
  )
}
