package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestFSMRef, TestProbe}
import cats.data.Writer
import cats.effect.IO
import cats.implicits._
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.{DeckListCard, Edition, InventoryCard, NonFoil}
import vdx.stockpile.Deck.{DeckLoaderResult, DeckLog}
import vdx.stockpile.Inventory.{InventoryError, InventoryLog}
import vdx.stockpile.cli.Core.FileDeckLoader
import vdx.stockpile.cli.UI.WorkerFinished
import vdx.stockpile.instances.eq._
import vdx.stockpile.{CardList, Deck, Inventory}

import scala.concurrent.duration._

class CoreSpec extends FlatSpec with Matchers {
  private type LoggedInventory[A] = Writer[Vector[InventoryLog], A]
  private type LoggedDeck[A] = Writer[Vector[DeckLog], A]

  implicit val system: ActorSystem = ActorSystem("test")

  private def defaultLoader(inventory: Inventory = CardList.empty[InventoryCard]) =
    (f: File) => IO({ inventory.pure[LoggedInventory] })

  private def defaultDeckLoader(decks: List[Deck[DeckListCard]] = List.empty) =
    new FileDeckLoader[DeckListCard] {
      var _decks = decks

      override def load(file: File): IO[DeckLoaderResult[DeckListCard]] = _decks match {
        case head :: tail =>
          _decks = tail
          head.pure[LoggedDeck].pure[IO]
        case _ => fail()
      }
    }

  private def fsm(inventory: Inventory = CardList.empty[InventoryCard], decks: List[Deck[DeckListCard]] = List.empty) =
    TestFSMRef(
      new Core(
        defaultLoader(inventory),
        defaultDeckLoader(decks)
      )
    )

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
    val core =
      parent.childActorOf(
        Props(
          classOf[Core],
          (f: File) => IO({ CardList.empty[InventoryCard].writer(logs) }),
          new FileDeckLoader[DeckListCard] {
            override def load(file: File): IO[DeckLoaderResult[DeckListCard]] = ???
          }
        )
      )
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
    val parent = TestProbe()
    val core = parent.childActorOf(Props(classOf[Core], defaultLoader(), new FileDeckLoader[DeckListCard] {
      override def load(file: File): IO[DeckLoaderResult[DeckListCard]] = ???
    }))
    parent.send(core, Core.LoadInventory(new File("")))
    parent.send(core, Core.PrintInventory)

    parent.fishForMessage(1.second, "") {
      case WorkerFinished(UI.InventoryResult(i)) => true
      case _                                     => false
    } shouldBe an[WorkerFinished]
  }

  "Core :: LoadDecks" should "a single deck file" in {
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4))
    val core = fsm(
      CardList.empty,
      List(
        Deck(
          mainBoard = mainBoard
        )
      )
    )

    core ! Core.LoadInventory(new File(""))
    core ! Core.LoadDecks(new File(""))

    core.stateData match {
      case Core.StateData(_, decks: List[Deck[_]]) => decks.head.mainBoard should equal(mainBoard)
      case _                                       => fail()
    }
  }

  it should "notify it's parent when the decks are loaded" in {
    val parent = TestProbe()
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4))
    val core = parent.childActorOf(
      Props(classOf[Core], defaultLoader(), defaultDeckLoader(List(Deck(mainBoard = mainBoard))))
    )

    parent.send(core, Core.LoadInventory(new File("")))
    core ! Core.LoadDecks(new File(""))

    parent.fishForMessage(1.second, "") {
      case WorkerFinished(UI.DecksAreLoaded) => true
      case _                                 => false
    } shouldBe an[WorkerFinished]
  }

  "Core :: DistinctHaves" should "return a list of haves for each loaded deck" in {

    val parent = TestProbe()
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4), DeckListCard("Forest", 1))
    val inventory = CardList(
      InventoryCard("Tarmogoyf", 2, Edition("MM3"), NonFoil),
      InventoryCard("Path to Exile", 3, Edition("MM3"), NonFoil),
      InventoryCard("Cavern of Souls", 3, Edition("MM3"), NonFoil)
    )

    val core = parent.childActorOf(
      Props(classOf[Core], defaultLoader(inventory), defaultDeckLoader(List(Deck(mainBoard = mainBoard))))
    )

    core ! Core.LoadInventory(new File(""))
    core ! Core.LoadDecks(new File(""))

    core ! Core.DistinctHaves

    parent
      .fishForMessage(1.second, "") {
        case WorkerFinished(haves: UI.DistinctHaves[DeckListCard]) => true
        case _                                                     => false
      } match {
      case WorkerFinished(haves: UI.DistinctHaves[DeckListCard]) =>
        haves.haves.head.toList should equal(
          CardList(
            DeckListCard("Tarmogoyf", 2),
            DeckListCard("Path to Exile", 3)
          ).toList
        )
      case _ => fail()

    }
  }
}
