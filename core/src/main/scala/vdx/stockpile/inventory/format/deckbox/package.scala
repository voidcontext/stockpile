package vdx.stockpile.inventory.format

import java.io.File

import cats.Monad
import cats.data.Validated.{Invalid, Valid}
import cats.data.{Validated, Writer}
import cats.syntax.writer
import cats.implicits._
import kantan.csv.ops._
import kantan.csv.{ReadResult, RowDecoder, rfc}
import vdx.stockpile.Card._
import vdx.stockpile.CardDB.RepositoryAlg
import vdx.stockpile.Inventory._
import vdx.stockpile.instances.eq._
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
    db: RepositoryAlg[F]
  ) extends InventoryLoaderAlg[F] {

    private type CSVResult = ReadResult[RawDeckboxCard]
    private type ValidatedResult = Validated[InventoryLoaderLog, RawDeckboxCard]
    private type Logged[A] = Writer[Vector[InventoryLoaderLog], A]

    override def load: F[Writer[Vector[InventoryLoaderLog], Inventory]] = {
      def foil: PartialFunction[Option[String], FoilState] = {
        case Some("foil") => Foil
        case _            => NonFoil
      }

      def mapEditionAndValidate: PartialFunction[CSVResult, F[ValidatedResult]] = {
        case Right(card) =>
          db.findSimpleSetByName(card.edition)
            .map({
              case Some(simpleSet) => Valid(card.copy(edition = simpleSet.code))
              case None            => Invalid(InventoryError(s"Cannot find set for ${card.toString}"))
            })
        case Left(error) =>
          Validated.invalid[InventoryLoaderLog, RawDeckboxCard](InventoryError(error.getMessage)).pure[F]
      }

      def appendRawCardToList(inventory: Inventory, card: RawDeckboxCard) =
        inventory.combine(
          CardList(
            InventoryCard(card.name, card.count, Edition(card.edition), foil(card.foil))
          )
        )

      def validatedResultToCardList(result: List[ValidatedResult]) =
        result.foldLeft(
          CardList.empty[InventoryCard].pure[Logged]
        ) {
          case (w, Valid(card)) =>
            w.map(appendRawCardToList(_, card))
          case (w, Invalid(error)) =>
            w.tell(Vector(error))
        }

      def buildInventory(rawCards: List[CSVResult]) =
        rawCards
          .traverse[F, ValidatedResult](mapEditionAndValidate)
          .map(validatedResultToCardList)

      for {
        cards <- parser.parse()
        inventory <- buildInventory(cards)
      } yield inventory
    }
  }
}
