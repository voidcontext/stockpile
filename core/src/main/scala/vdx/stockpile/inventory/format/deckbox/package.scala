package vdx.stockpile.inventory

import cats.effect.IO
import cats.syntax.either._
import kantan.codecs.Result.{Failure, Success}
import kantan.csv.ops._
import kantan.csv.{RowDecoder, rfc}
import vdx.stockpile.Card._
import vdx.stockpile.{CardList, Inventory}
import vdx.stockpile.Inventory._
import vdx.stockpile.instances._
import java.io.File

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}

package object deckbox {
  final case class RawDeckboxCard(
    count: Int,
    name: String,
    edition: String,
    cardNumber: Int,
    condition: String,
    language: String,
    foil: Option[String]
  )

  private[deckbox] implicit val cardDecoder: RowDecoder[RawDeckboxCard] =
    RowDecoder.decoder(0, 2, 3, 4, 5, 6, 7)(RawDeckboxCard.apply)

  class IOReaderInterpreter(file: File) extends InventoryReaderAlg[IO] {
    override def read: IO[ValidatedNel[InventoryError, Inventory]] =
      IO({
        val parsedCsv = file.toURI.toURL.readCsv[List, RawDeckboxCard](rfc.withHeader)

        def foil(state: Option[String]): FoilState = state match {
          case Some("foil") => Foil
          case _            => NonFoil
        }

        parsedCsv
          .foldLeft[ValidatedNel[InventoryError, Inventory]](Valid(CardList.empty[InventoryCard]))({
            case (list, Success(card)) =>
              list.map(l => l.add(InventoryCard(card.name, card.count, Edition(card.edition), foil(card.foil))))
            case (Valid(_), Failure(message)) =>
              Invalid(NonEmptyList.one(InventoryError(message.getMessage)))
            case (errList, Failure(message)) =>
              errList.leftMap(errList => errList.prepend(InventoryError(message.getMessage)))
          })
      })
  }

}
