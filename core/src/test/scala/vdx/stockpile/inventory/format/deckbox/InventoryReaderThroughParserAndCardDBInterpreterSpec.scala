package vdx.stockpile.inventory.format.deckbox

import java.io.File

import cats.data.Validated.{Invalid, Valid}
import cats.effect.IO
import kantan.csv.ReadResult
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.CardDB
import vdx.stockpile.CardDB.{RepositoryAlg, SimpleSet}

class InventoryReaderThroughParserAndCardDBInterpreterSpec extends FlatSpec with Matchers {
  val db = new RepositoryAlg[IO] {
    override def findSimpleSetByName(name: String): IO[Option[CardDB.SimpleSet]] =
      IO({
        if (name == "Masters 25") Option(SimpleSet(name = "Masters25", code = "A25")) else None
      })
  }

  val parser = new CsvParserAlg[IO] {
    override def file: File = new File(
      getClass.getResource("/deckbox-inventory.csv").getPath
    )

    override def parse(): IO[List[ReadResult[RawDeckboxCard]]] = IO({ parseRaw() })
  }

  val reader = new InventoryReaderThroughParserAndCardDBInterpreter[IO](
    parser,
    db,
    IO.apply(_)
  )

  "Deckbox IORederInterpreter" should "import csv file" in {

    (reader.read.unsafeRunSync().value.toList should have).length(81)
  }

  it should "translate edition names to edition codes" in {
    reader.read.unsafeRunSync().value.toList.head.edition.code should be("A25")
  }

  it should "add a message when a card's set doesn't exist" in {
    reader.read.unsafeRunSync().written.head.getMessage should contain("Cannot find set")
  }
}
