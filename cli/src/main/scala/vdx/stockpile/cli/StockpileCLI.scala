package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import cats.effect.IO
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.Deck.DeckLoaderResult
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cli.Core.FileDeckLoader
import vdx.stockpile.cli.console.Terminal
import vdx.stockpile.deck.format.decklist.DeckListFromFileIOInterpreter
import vdx.stockpile.inventory.format.deckbox.{IOCsvParserInterpreter, InventoryReaderThroughParserAndCardDBInterpreter}
import vdx.stockpile.mgjson.MtgJsonDBInterpreter

import scala.io.Source

object StockpileCLI extends App {

  val system = ActorSystem()

  def loadInventoryFromFile: File => IO[InventoryLoaderResult] = (csvFile: File) => {
    new InventoryReaderThroughParserAndCardDBInterpreter[IO](
      new IOCsvParserInterpreter(csvFile),
      new MtgJsonDBInterpreter {}
    ).load
  }

  val fileDeckLoader = new FileDeckLoader[DeckListCard] {
    override def load(file: File): IO[DeckLoaderResult[DeckListCard]] =
      new DeckListFromFileIOInterpreter(IO({ Source.fromFile(file, "UTF-8") })).load
  }

  val ui = system.actorOf(
    Props(
      classOf[UI],
      (system: ActorRefFactory) => system.actorOf(Props(classOf[Core], loadInventoryFromFile, fileDeckLoader)),
      new Terminal
    ),
    "ui"
  )
}
