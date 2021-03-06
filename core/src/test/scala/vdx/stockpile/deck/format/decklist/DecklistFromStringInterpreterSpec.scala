package vdx.stockpile.deck.format.decklist

import cats.Id
import cats.syntax.foldable._
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.{Deck, SampleDeck}

class DecklistFromStringInterpreterSpec extends FlatSpec with Matchers with SampleDeck {
  val decklist = scala.io.Source.fromResource(sampleDeckResource).mkString

  "DecklistInterpreter" should "load a decklist from string" in {
    val loader = new DecklistFromStringInterpreter[Id]("dummy deck", decklist)

    val (_, deck) = loader.load.run

    deck shouldBe an[Deck[DeckListCard]]
  }

  it should "populate the main board" in {
    val loader = new DecklistFromStringInterpreter[Id]("dummy deck", decklist)

    val (_, deck) = loader.load.run

    deck.mainBoard.toList should equal(sampleDeck.mainBoard.toList)
  }

  it should "populate the side board" in {
    val loader = new DecklistFromStringInterpreter[Id]("dummy deck", decklist)

    val (_, deck) = loader.load.run

    deck.sideBoard.toList should equal(sampleDeck.sideBoard.toList)
  }
}
