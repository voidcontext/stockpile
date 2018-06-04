package vdx.stockpile.inventory.format.deckbox

import java.io.File

import cats.data.Validated.{Invalid, Valid}
import cats.effect.IO
import kantan.csv.ReadResult
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.PreReleaseFoil
import vdx.stockpile.CardDB
import vdx.stockpile.CardDB.{RepositoryAlg, SimpleSet}
import vdx.stockpile.cardlist.CardListInstances
import cats.syntax.foldable._

class InventoryReaderThroughParserAndCardDBInterpreterSpec extends FlatSpec with Matchers with CardListInstances {
  val db = new RepositoryAlg[IO] {
    override def findSimpleSetByName(name: String): IO[Option[CardDB.SimpleSet]] =
      IO({
        name match {
          case "Masters 25"          => Option(SimpleSet(name = "Masters25", code = "A25"))
          case "Hour of Devastation" => Option(SimpleSet(name = "Hour of Devastation", code = "HOU"))
          case _                     => None
        }
      })
  }

  def reader(fileName: String = "/deckbox-inventory.csv") =
    new InventoryReaderThroughParserAndCardDBInterpreter[IO](
      new CsvParserAlg[IO] {
        override def file: File = new File(
          getClass.getResource(fileName).getPath
        )

        override def parse(): IO[List[ReadResult[RawDeckboxCard]]] = IO({ parseRaw() })
      },
      db
    )

  "Deckbox IORederInterpreter" should "import csv file" in {
    val (_, value) = reader().load.unsafeRunSync().run

    (value.toList should have).length(81)
  }

  it should "translate edition names to edition codes" in {
    val (_, value) = reader().load.unsafeRunSync().run

    value.toList.head.edition.code should be("A25")
  }

  it should "add a message when a card's set doesn't exist" in {
    val (log, _) = reader().load.unsafeRunSync().run

    log.head.message should startWith("Cannot find set")
  }

  it should "set the correct edition of pre-release promo cards" in {
    val (log, inventory) = reader("/deckbox-inventory-prerelease.csv").load.unsafeRunSync().run

    log shouldBe empty
    inventory.toList.exists(_.name == "Majestic Myriarch") should be(true)
  }

  it should "set the correct foil state of pre-release promo cards" in {
    val (log, inventory) = reader("/deckbox-inventory-prerelease.csv").load.unsafeRunSync().run

    log shouldBe empty
    inventory.toList.exists(card => card.name == "Majestic Myriarch" && card.foil == PreReleaseFoil) should be(true)
  }
}
