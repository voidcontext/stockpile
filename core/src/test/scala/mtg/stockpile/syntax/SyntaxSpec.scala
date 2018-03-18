package mtg.stockpile.syntax

import mtg.stockpile.{DeckListCard, _}
import org.scalatest.{FlatSpec, Matchers}

class SyntaxSpec extends FlatSpec with Matchers {
  val deckList = DeckList(
    mainBoard = Set(
      DeckListCard("Tarmogoyf", 4)
    ),
    sideBoard = Set(
      DeckListCard("Path to Exile", 4),
      DeckListCard("Duress", 2)
    ),
    maybeBoard = Set(
      DeckListCard("Duress", 2)
    )
  )

  "DecklistOps' - method " should "decrease the count of the given card if it's in the mainboard" in {
    val card = DeckListCard("Tarmogoyf", 2)
    (deckList - card) should be(
      DeckList(
        mainBoard = Set(
          DeckListCard("Tarmogoyf", 2)
        ),
        sideBoard = Set(
          DeckListCard("Path to Exile", 4),
          DeckListCard("Duress", 2)
        ),
        maybeBoard = Set(
          DeckListCard("Duress", 2)
        )
      )
    )
  }

  it should "decrease the count of the given card if it's in the sideboard" in {
    val card = DeckListCard("Path to Exile", 3)

    (deckList - card) should be(
      DeckList(
        mainBoard = Set(
          DeckListCard("Tarmogoyf", 4)
        ),
        sideBoard = Set(
          DeckListCard("Path to Exile", 1),
          DeckListCard("Duress", 2)
        ),
        maybeBoard = Set(
          DeckListCard("Duress", 2)
        )
      )
    )
  }

  it should "decrease the count of the given card if it's in multiple boards" in {
    val card = DeckListCard("Duress", 3)

    (deckList - card) should be(
      DeckList(
        mainBoard = Set(
          DeckListCard("Tarmogoyf", 4)
        ),
        sideBoard = Set(
          DeckListCard("Path to Exile", 4)
        ),
        maybeBoard = Set(
          DeckListCard("Duress", 1)
        )
      )
    )

  }

  "InventoryOps"
}
