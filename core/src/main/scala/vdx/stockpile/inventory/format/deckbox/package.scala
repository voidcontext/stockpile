package vdx.stockpile.inventory.format

import java.io.File

import cats.Monad
import cats.data.Validated.{Invalid, Valid}
import cats.data.{Validated, Writer}
import cats.implicits._
import kantan.codecs.Result.{Failure, Success}
import kantan.csv.ops._
import kantan.csv.{ReadResult, RowDecoder, rfc}
import vdx.stockpile.Card._
import vdx.stockpile.CardDB.RepositoryAlg
import vdx.stockpile.Inventory._
import vdx.stockpile.instances._
import vdx.stockpile.{CardList, Inventory}

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

  trait CsvParserAlg[F[_]] {
    protected def parseRaw(): List[ReadResult[RawDeckboxCard]] =
      file.toURI.toURL.readCsv[List, RawDeckboxCard](rfc.withHeader)

    def file: File

    def parse(): F[List[ReadResult[RawDeckboxCard]]]
  }

  class InventoryReaderThroughParserAndCardDBInterpreter[F[_]: Monad](
    parser: CsvParserAlg[F],
    db: RepositoryAlg[F],
    liftToF: Validated[InventoryReaderLog, RawDeckboxCard] => F[Validated[InventoryReaderLog, RawDeckboxCard]]
  ) extends InventoryReaderAlg[F] {
    private[this] type CSVResult = ReadResult[RawDeckboxCard]

    override def read: F[Writer[Vector[InventoryReaderLog], Inventory]] = {
      def foil: PartialFunction[Option[String], FoilState] = {
        case Some("foil") => Foil
        case _            => NonFoil
      }

      def mapEdition: PartialFunction[CSVResult, F[Validated[InventoryReaderLog, RawDeckboxCard]]] = {
        case Success(card) =>
          db.findSimpleSetByName(card.edition)
            .map({
              case Some(simpleSet) => Valid(card.copy(edition = simpleSet.code))
              case None            => Invalid(InventoryError(s"Cannot find set for ${card.toString}"))
            })
        case Failure(error) => liftToF(Invalid(InventoryError(error.getMessage)))
      }

      def appendRawCardToList(w: Writer[Vector[InventoryReaderLog], Inventory], card: RawDeckboxCard) =
        w.map(
          _.add(
            InventoryCard(card.name, card.count, Edition(card.edition), foil(card.foil))
          )
        )

      def buildInventory(rawCards: List[ReadResult[RawDeckboxCard]]) =
        rawCards
          .traverse[F, Validated[InventoryReaderLog, RawDeckboxCard]](mapEdition)
          .map(
            _.foldLeft(Writer(Vector.empty[InventoryReaderLog], CardList.empty[InventoryCard]))({
              case (w, Valid(card)) =>
                appendRawCardToList(w, card)
              case (w, Invalid(error)) =>
                w.tell(Vector(InventoryError(error.getMessage)))
            })
          )

      for {
        cards <- parser.parse()
        inventory <- buildInventory(cards)
      } yield inventory
    }
  }

}
