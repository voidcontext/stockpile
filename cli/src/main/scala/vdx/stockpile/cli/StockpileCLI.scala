package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorSystem, Props}
import cats.effect.IO
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cli.console.Terminal
import vdx.stockpile.inventory.format.deckbox.{IOCsvParserInterpreter, InventoryReaderThroughParserAndCardDBInterpreter}
import vdx.stockpile.mgjson.MtgJsonDBInterpreter

object StockpileCLI extends App {

  val system = ActorSystem()

  val coreFsm = system.actorOf(Props[CoreFSM], "core")
  val uiFsm = system.actorOf(Props[UIFSM], "ui")

  def loadInventoryFromFile: File => IO[InventoryLoaderResult] = (csvFile: File) => {
    new InventoryReaderThroughParserAndCardDBInterpreter[IO](
      new IOCsvParserInterpreter(csvFile),
      new MtgJsonDBInterpreter {}
    ).load
  }

  uiFsm ! UISpec.Initialize(
    { case Menu.InventoryExportTerminal => coreFsm ! CoreSpec.PrintInventory },
    new Terminal
  )
  coreFsm ! CoreSpec.Initialize(uiFsm, loadInventoryFromFile)
  coreFsm ! CoreSpec.LoadInventory(loadLastInventoryFromDownloads())
}
