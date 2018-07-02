package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestFSMRef, TestProbe}
import cats.Id
import cats.data.Writer
import cats.effect.IO
import cats.implicits._
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.{DeckListCard, Edition, InventoryCard, NonFoil}
import vdx.stockpile.Deck.{DeckLoaderResult, DeckLog}
import vdx.stockpile.Inventory.{InventoryError, InventoryLoaderResult, InventoryLog}
import vdx.stockpile.cardlist.CardList
import vdx.stockpile.cli.Core.{FileDeckLoader, FileDeckLoaderResult}
import vdx.stockpile.instances.eq._
import vdx.stockpile.{Deck, Inventory, DeckInterpreter}

import scala.concurrent.duration._

class CoreSpec extends FlatSpec with Matchers {
  private type LoggedInventory[A] = Writer[Vector[InventoryLog], A]
  private type LoggedDeck[A] = Writer[Vector[DeckLog], A]

  implicit val system: ActorSystem = ActorSystem("test")

  implicit val idExtractor: Extractor[Id] = new Extractor[Id] {
    override def extract[A](fa: Id[A]): A = fa
  }

  private def defaultLoader(
    inventory: Inventory = CardList.empty[InventoryCard]
  ): File => Id[LoggedInventory[Inventory]] =
    (f: File) => inventory.pure[LoggedInventory]

  private def defaultDeckLoader(decks: List[Deck[DeckListCard]] = List.empty) =
    new FileDeckLoader[DeckListCard, Id] {
      var _decks = decks

      override def load(file: File): Id[FileDeckLoaderResult[DeckListCard]] = _decks match {
        case head :: tail =>
          _decks = tail
          List(head).pure[LoggedDeck].pure[Id]
        case _ => fail()
      }
    }

  private def fsm(
    inventory: Inventory = CardList.empty[InventoryCard],
    decks: List[Deck[DeckListCard]] = List.empty
  ): TestFSMRef[Core.State, Core.Data, Core[Id, Id]] = {
    implicit val coreContext: Core.CoreContext[Id, Id] = Core.CoreContext(
      defaultLoader(inventory),
      defaultDeckLoader(decks),
      new DeckInterpreter {}
    )

    TestFSMRef(new Core())
  }

  "Core" should "start in Unintialised state" in {
    val core = fsm()

    core.stateName should be(Core.Uninitialized)
    core.stateData should be(Core.Empty)
  }

  private def loadInventory() = {
    val inventory = CardList(InventoryCard("Path to Exile", 4, Edition("CON"), NonFoil))
    val core = fsm(inventory)

    core ! Core.LoadInventory(new File(""))
    (core, inventory)
  }

  it should "load the inventory when it receives the LoadInventory message and it is initialized" in {
    val (core, inventory) = loadInventory()

    core.stateData match {
      case Core.StateData(Some(i), _) => i should equal(inventory)
      case _                          => fail()
    }
  }

  it should "notify it's parent when the inventory is loaded" in {
    val parent = TestProbe()
    val logs = Vector(InventoryError("Warning 1"), InventoryError("Warning 2"))

    implicit val coreContext: Core.CoreContext[Id, Id] = Core.CoreContext[Id, Id](
      (f: File) => CardList.empty[InventoryCard].writer(logs),
      (file: File) => ???,
      new DeckInterpreter {}
    )

    val core = parent.childActorOf(Props(new Core[Id, Id]()))

    parent.send(core, Core.LoadInventory(new File("")))

    parent.fishForMessage(1.second, "") {
      case UI.InventoryAvailable(_) => true
      case _                        => false
    } match {
      case UI.InventoryAvailable(l) => l should equal(logs)
      case _                        => fail()
    }
  }

  it should "change it state to InventoryLoaded after loading the inventory" in {
    val (core, _) = loadInventory()
    core.stateName should be(Core.InventoryLoaded)
  }

  it should "notify it's parent when a result is available" in {
    implicit val coreContext: Core.CoreContext[Id, Id] = Core.CoreContext(
      defaultLoader(),
      (file: File) => ???,
      new DeckInterpreter {}
    )

    val parent = TestProbe()
    val core = parent.childActorOf(Props(new Core[Id, Id]()))

    parent.send(core, Core.LoadInventory(new File("")))
    parent.send(core, Core.PrintInventory)

    parent.fishForMessage(1.second, "") {
      case UI.InventoryResult(_) => true
      case _                     => false
    } shouldBe an[UI.InventoryResult]
  }

  "Core :: LoadDecks" should "a single deck file" in {
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4))
    val core = fsm(
      CardList.empty,
      List(
        Deck(
          "dummy deck",
          mainBoard = mainBoard
        )
      )
    )

    core ! Core.LoadInventory(new File(""))
    core ! Core.LoadDecks(new File(""))

    core.stateData match {
      case Core.StateData(_, decks) => decks.head.mainBoard should equal(mainBoard)
      case _                        => fail()
    }
  }

  it should "notify it's parent when the decks are loaded" in {
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4))

    implicit val coreContext: Core.CoreContext[Id, Id] = Core.CoreContext(
      defaultLoader(),
      defaultDeckLoader(List(Deck("dummy deck", mainBoard = mainBoard))),
      new DeckInterpreter {}
    )

    val parent = TestProbe()

    val core = parent.childActorOf(Props(new Core[Id, Id]()))

    parent.send(core, Core.LoadInventory(new File("")))
    core ! Core.LoadDecks(new File(""))

    parent.fishForMessage(1.second, "") {
      case UI.DecksAreLoaded => true
      case _                 => false
    } should equal(UI.DecksAreLoaded)
  }

  "Core :: DistinctHaves" should "return a list of haves for each loaded deck" in {

    val parent = TestProbe()
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4), DeckListCard("Forest", 1))
    val inventory = CardList(
      InventoryCard("Tarmogoyf", 2, Edition("MM3"), NonFoil),
      InventoryCard("Path to Exile", 3, Edition("MM3"), NonFoil),
      InventoryCard("Cavern of Souls", 3, Edition("MM3"), NonFoil)
    )

    implicit val coreContext: Core.CoreContext[Id, Id] = Core.CoreContext(
      defaultLoader(inventory),
      defaultDeckLoader(List(Deck("dummy deck", mainBoard = mainBoard))),
      new DeckInterpreter {}
    )

    val core = parent.childActorOf(Props(new Core[Id, Id]()))

    core ! Core.LoadInventory(new File(""))
    core ! Core.LoadDecks(new File(""))

    core ! Core.DistinctHaves

    parent
      .fishForMessage(1.second, "") {
        case UI.DistinctHaves(_) => true
        case _                   => false
      } match {
      case UI.DistinctHaves(haves) =>
        haves.head.haves.toList should equal(
          CardList(
            DeckListCard("Tarmogoyf", 2),
            DeckListCard("Path to Exile", 3)
          ).toList
        )
      case _ => fail()
    }
  }

  "Core :: DistinctMissing" should "return a list of haves for each loaded deck" in {

    val parent = TestProbe()
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4), DeckListCard("Forest", 1))
    val inventory = CardList(
      InventoryCard("Tarmogoyf", 2, Edition("MM3"), NonFoil),
      InventoryCard("Path to Exile", 3, Edition("MM3"), NonFoil),
      InventoryCard("Cavern of Souls", 3, Edition("MM3"), NonFoil)
    )

    implicit val coreContext: Core.CoreContext[Id, Id] = Core.CoreContext(
      defaultLoader(inventory),
      defaultDeckLoader(List(Deck("dummy deck", mainBoard = mainBoard))),
      new DeckInterpreter {}
    )

    val core = parent.childActorOf(Props(new Core[Id, Id]()))

    core ! Core.LoadInventory(new File(""))
    core ! Core.LoadDecks(new File(""))

    core ! Core.DistinctMissing

    parent
      .fishForMessage(1.second, "") {
        case UI.DistinctMissing(_) => true
        case _                     => false
      } match {
      case UI.DistinctMissing(missing) =>
        missing.head.missing.toList should equal(
          CardList(
            DeckListCard("Tarmogoyf", 2),
            DeckListCard("Path to Exile", 1),
            DeckListCard("Forest", 1)
          ).toList
        )
      case _ => fail()
    }
  }
}
