package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import cats.{Bimonad, Comonad, Id, Monad}
import cats.data.Writer
import cats.effect.IO
import cats.implicits._
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.{Deck, DeckInterpreter}
import vdx.stockpile.Deck.DeckLog
import vdx.stockpile.Inventory.InventoryLoaderResult
import vdx.stockpile.cli.Core.{FileDeckLoader, FileDeckLoaderResult}
import vdx.stockpile.cli.console.Terminal
import vdx.stockpile.deck.format.decklist.DeckListFromFileIOInterpreter
import vdx.stockpile.instances.cardlist._
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

  val fileDeckLoader = new FileDeckLoader[DeckListCard, IO] {
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
      (system: ActorRefFactory) => {
        implicit val ioExtractor: Extractor[IO] = new Extractor[IO] {
          override def extract[A](fa: IO[A]): A = fa.unsafeRunSync()
        }

        implicit val idExtractor: Extractor[Id] = new Extractor[Id] {
          override def extract[A](fa: Id[A]): A = fa
        }
        implicit val coreCtx: Core.CoreContext[Id, IO] =
          Core.CoreContext(loadInventoryFromFile, fileDeckLoader, new DeckInterpreter {})
        system.actorOf(Props(new Core[Id, IO]()))
      },
      new Terminal
    ),
    "ui"
  )
}
