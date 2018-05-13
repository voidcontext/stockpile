package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import cats.data.Writer
import cats.effect.IO
import cats.implicits._
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.Deck
import vdx.stockpile.Deck.{DeckLoaderResult, DeckLog}
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cli.Core.{FileDeckLoader, FileDeckLoaderResult}
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
    private val regex = """\.txt$""".r

    override def load(file: File): IO[FileDeckLoaderResult[DeckListCard]] = {
      type Logged[A] = Writer[Vector[DeckLog], A]

      def load1(file: File) =
        new DeckListFromFileIOInterpreter(file.getName, IO({
          Source.fromFile(file, "UTF-8")
        })).load.map(result => result.map(List.apply(_)))

      def loadDir(dir: File) =
        dir
          .listFiles()
          .toList
          .filter(f => f.isFile && regex.findFirstMatchIn(f.getName).isDefined)
          .foldLeft(IO.pure(List.empty[Deck[DeckListCard]].pure[Logged])) { (resultIO, file) =>
            resultIO.flatMap { result =>
              load1(file).map { loadResult =>
                result.flatMap(decks => loadResult.map(decks ++ _))
              }
            }
          }

      if (file.isDirectory) loadDir(file)
      else load1(file)
    }
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
