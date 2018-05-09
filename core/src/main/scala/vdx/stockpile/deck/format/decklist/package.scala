package vdx.stockpile.deck.format

import java.io.File

import cats.{Applicative, Monad}
import cats.data.Writer
import cats.effect.IO
import cats.implicits._
import cats.syntax.applicative._
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.{CardList, Deck}
import vdx.stockpile.Deck.{DeckLoaderAlg, DeckLoaderResult, DeckLog, ParserError}
import vdx.stockpile.instances.eq._

import scala.io.Source
import scala.util.matching.Regex

package object decklist {
  class DeckListFromFileIOInterpreter(fileIO: IO[Source]) extends DeckLoaderAlg[IO, DeckListCard] {
    override def load: IO[DeckLoaderResult[DeckListCard]] =
      fileIO
        .map(_.getLines.mkString("\n"))
        .flatMap(
          loadedString => new DecklistFromStringInterpreter[IO](loadedString).load
        )

  }

  class DecklistFromStringInterpreter[F[_]: Monad](deckList: String) extends DeckLoaderAlg[F, DeckListCard] {
    private def lineRegex = """^(\d+)(x|) (.*)$""".r

    type Logged[A] = Writer[Vector[DeckLog], A]

    private def matchesToCard(regexMatch: Regex.Match) = DeckListCard(regexMatch.group(3), regexMatch.group(1).toInt)

    private def count(list: CardList[DeckListCard]) = list.foldLeft(0)((count, card) => count + card.count)

    override def load: F[DeckLoaderResult[DeckListCard]] =
      Applicative[F].pure(
        deckList
          .split("\n")
          .foldLeft(Applicative[Logged].pure(Deck[DeckListCard]()))(
            (w, line) =>
              lineRegex.findFirstMatchIn(line) match {
                case Some(regexMatch) =>
                  w.map(
                    deck =>
                      if (count(deck.mainBoard) < 60) {
                        deck.copy(mainBoard = deck.mainBoard.combine(CardList(matchesToCard(regexMatch))))
                      } else {
                        deck.copy(sideBoard = deck.sideBoard.combine(CardList(matchesToCard(regexMatch))))
                    }
                  )
                case None =>
                  w.tell(Vector(ParserError(s"Cannot parse line: $line")))
            }
          )
      )
  }
}
