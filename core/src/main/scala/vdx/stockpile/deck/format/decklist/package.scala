package vdx.stockpile.deck.format

import cats.data.Writer
import cats.effect.IO
import cats.implicits._
import cats.{Applicative, Monad}
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.Deck
import vdx.stockpile.Deck.{DeckLoaderAlg, DeckLoaderResult, DeckLog, ParserError}
import vdx.stockpile.cardlist.{CardList, CardListFoldable, CardListMonoid}
import vdx.stockpile.instances.cardlist._

import scala.io.Source
import scala.util.matching.Regex

package object decklist {
  class DeckListFromFileIOInterpreter(name: String, fileIO: IO[Source]) extends DeckLoaderAlg[IO, DeckListCard] {
    override def load: IO[DeckLoaderResult[DeckListCard]] =
      fileIO
        .map(_.getLines.mkString("\n"))
        .flatMap(
          loadedString => new DecklistFromStringInterpreter[IO](name, loadedString).load
        )

  }

  class DecklistFromStringInterpreter[F[_]: Monad](name: String, deckList: String)(
    implicit clf: CardListFoldable,
    clm: CardListMonoid[DeckListCard]
  ) extends DeckLoaderAlg[F, DeckListCard] {
    private def lineRegex = """^(\d+)(x|) (.*)$""".r

    type Logged[A] = Writer[Vector[DeckLog], A]

    private def matchesToCard(regexMatch: Regex.Match) = DeckListCard(regexMatch.group(3), regexMatch.group(1).toInt)

    private def count(list: CardList[DeckListCard]) =
      clf.foldLeft(list, 0)((count, card) => count + card.count)

    override def load: F[DeckLoaderResult[DeckListCard]] =
      Applicative[F].pure(
        deckList
          .split("\n")
          .foldLeft(Applicative[Logged].pure(Deck[DeckListCard](name)))(
            (w, line) =>
              lineRegex.findFirstMatchIn(line) match {
                case Some(regexMatch) =>
                  w.map(
                    deck =>
                      if (count(deck.mainBoard) < 60) {
                        deck.copy(mainBoard = clm.combine(deck.mainBoard, CardList(matchesToCard(regexMatch))))
                      } else {
                        deck.copy(sideBoard = clm.combine(deck.sideBoard, CardList(matchesToCard(regexMatch))))
                    }
                  )
                case None =>
                  w.tell(Vector(ParserError(s"Cannot parse line: $line")))
            }
          )
      )
  }
}
