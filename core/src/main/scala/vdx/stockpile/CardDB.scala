package vdx.stockpile

object CardDB {
  case class Card(name: String, id: String) {
    def withSet(set: SimpleSet) = CardWithSimpleSet(this, set)
  }

  case class CardWithSimpleSet(card: Card, set: SimpleSet)

  case class SimpleSet(name: String, code: String)

  case class CardSet(name: String, code: String, cards: List[Card]) {
    def asSimple = SimpleSet(
      name,
      code
    )
  }

  trait RepositoryAlg[F[_]] {
    def findSimpleSetByName(name: String): F[Option[SimpleSet]]
  }
}
