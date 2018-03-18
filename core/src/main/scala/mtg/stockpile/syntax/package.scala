package mtg.stockpile

package object syntax {

  private[syntax] trait Ops[A <: Card] {
    def withCount(instance: A, count: Int): A
  }

  private[syntax] implicit val deckListOps = new Ops[DeckListCard] {
    override def withCount(deckListCard: DeckListCard, count: Int): DeckListCard =
      deckListCard.copy(count = count)
  }

  case class State[A <: Card](set: Set[A], remaining: Int)
  private[syntax] def removeCardFromSet[A <: Card, B <: Card](set: Set[A], card: B)(implicit cardOps: Ops[A]): State[A] = {

    set
      .foldLeft(State(Set.empty[A], card.count))(
        (state, setCard) => {
          if (setCard.name != card.name || state.remaining == 0) State(state.set + setCard, state.remaining)
          else {
            val diff = Math.min(state.remaining, setCard.count)
            val remainingCard = cardOps.withCount(setCard, setCard.count - diff)
            State(
              if (remainingCard.count > 0) state.set + remainingCard else state.set,
              state.remaining - diff
            )
          }
        }
      )
  }

  private[syntax] def removeSetFromSet[A <: Card, B <: Card](setA: Set[A], setB: Set[B]): Set[A] = {
    setB.foldLeft(setA)((set, card) => {
      removeCardFromSet(set, card).set
    })
  }

  implicit class DeckOps(deckList: DeckList) {
    def -(card: DeckListCard): DeckList = {
      val mainBoard = removeCardFromSet(deckList.mainBoard, card)
      val sideBoard =
        removeCardFromSet(deckList.sideBoard, implicitly[Ops[DeckListCard]].withCount(card, mainBoard.remaining))
      val maybeBoard =
        removeCardFromSet(deckList.maybeBoard, implicitly[Ops[DeckListCard]].withCount(card, sideBoard.remaining))
      DeckList(
        mainBoard = mainBoard.set,
        sideBoard = sideBoard.set,
        maybeBoard = maybeBoard.set
      )
    }
  }

  implicit class InventoryOps(inventory: Inventory) {
    def -(deckList: DeckList): Inventory =
      inventory.copy(
        cards = removeSetFromSet(inventory.cards, deckList.mainBoard ++ deckList.sideBoard ++ deckList.maybeBoard)
      )
  }
}
