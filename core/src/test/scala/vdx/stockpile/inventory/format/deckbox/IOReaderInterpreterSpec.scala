package vdx.stockpile.inventory.format.deckbox

import java.io.File

import cats.data.Validated.{Invalid, Valid}
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.inventory.deckbox.IOReaderInterpreter

class IOReaderInterpreterSpec extends FlatSpec with Matchers {
  "Deckbox IORederInterpreter" should "import csv file" in {
    val reader = new IOReaderInterpreter(
      new File(
        getClass.getResource("/deckbox-inventory.csv").getPath
      )
    )

    reader.read.unsafeRunSync() match {
      case Valid(list)        => (list.toList should have).length(81)
      case Invalid(errorList) => fail(errorList.toString())
    }
  }
}
