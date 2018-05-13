package vdx.stockpile.deck.format.decklist

import java.io.File

import cats.Id
import cats.effect.IO
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.{Deck, SampleDeck}

class DeckListFromFileIOInterpreterSpec extends FlatSpec with Matchers with SampleDeck {
  val file = IO({ scala.io.Source.fromResource(sampleDeckResource) })

  "DeckListFromFileIOInterpreter" should "load a decklist from string" in {
    val loader = new DeckListFromFileIOInterpreter("dummy deck", file)

    val (_, deck) = loader.load.unsafeRunSync().run

    deck shouldBe an[Deck[DeckListCard]]
  }

  it should "populate the main board" in {
    val loader = new DeckListFromFileIOInterpreter("dummy deck", file)

    val (_, deck) = loader.load.unsafeRunSync().run

    deck.mainBoard.toList should equal(sampleDeck.mainBoard.toList)
  }

  it should "populate the side board" in {
    val loader = new DeckListFromFileIOInterpreter("dummy deck", file)

    val (_, deck) = loader.load.unsafeRunSync().run

    deck.sideBoard.toList should equal(sampleDeck.sideBoard.toList)
  }

}
