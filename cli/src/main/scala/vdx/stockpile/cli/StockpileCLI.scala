package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorSystem, Props}
import cats.effect.IO
import kantan.csv.ReadResult
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cli.console.Terminal
import vdx.stockpile.inventory.format.deckbox.{
  CsvParserAlg,
  InventoryReaderThroughParserAndCardDBInterpreter,
  RawDeckboxCard
}
import vdx.stockpile.mgjson.MtgJsonDBInterpreter

object StockpileCLI extends App {

  val system = ActorSystem()

  val coreFsm = system.actorOf(Props[CoreFSM], "core")
  val uiFsm = system.actorOf(Props[UIFSM], "ui")

  val db = new MtgJsonDBInterpreter {}

  def inventoryLoader: File => IO[InventoryLoaderResult] = (csvFile: File) => {
    val parser = new CsvParserAlg[IO] {
      override def file: File = csvFile
      override def parse(): IO[List[ReadResult[RawDeckboxCard]]] = IO({ parseRaw() })
    }

    new InventoryReaderThroughParserAndCardDBInterpreter[IO](
      parser,
      db
    ).load
  }

  uiFsm ! UISpec.Initialize(
    { case Menu.InventoryExportTerminal => coreFsm ! CoreSpec.PrintInventory },
    new Terminal
  )
  coreFsm ! CoreSpec.Initialize(uiFsm, inventoryLoader)
  coreFsm ! CoreSpec.LoadInventory(loadLastInventoryFromDownloads())
}
