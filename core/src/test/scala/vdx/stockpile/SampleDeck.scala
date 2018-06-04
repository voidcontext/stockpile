package vdx.stockpile

import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.cardlist.{CardList, CardListInstances}
import vdx.stockpile.instances.eq._

trait SampleDeck extends CardListInstances {
  val sampleDeckResource = "decklist.txt"

  val sampleDeck = Deck(
    "dummy deck",
    mainBoard = CardList(
      DeckListCard("Eldrazi Displacer", 4),
      DeckListCard("Flickerwisp", 4),
      DeckListCard("Leonin Arbiter", 4),
      DeckListCard("Thalia, Guardian of Thraben", 3),
      DeckListCard("Thought-Knot Seer", 4),
      DeckListCard("Tidehollow Sculler", 4),
      DeckListCard("Wasteland Strangler", 3),
      DeckListCard("Lingering Souls", 4),
      DeckListCard("Path to Exile", 4),
      DeckListCard("Aether Vial", 4),
      DeckListCard("Caves of Koilos", 4),
      DeckListCard("Concealed Courtyard", 4),
      DeckListCard("Eldrazi Temple", 4),
      DeckListCard("Ghost Quarter", 4),
      DeckListCard("Plains", 2),
      DeckListCard("Shambling Vent", 3),
      DeckListCard("Swamp", 1),
    ),
    sideBoard = CardList(
      DeckListCard("Bitterblossom", 1),
      DeckListCard("Blessed Alliance", 2),
      DeckListCard("Fatal Push", 2),
      DeckListCard("Gideon, Ally of Zendikar", 2),
      DeckListCard("Kambal, Consul of Allocation", 2),
      DeckListCard("Mirran Crusader", 2),
      DeckListCard("Orzhov Pontiff", 2),
      DeckListCard("Stony Silence", 2),
    )
  )

}
