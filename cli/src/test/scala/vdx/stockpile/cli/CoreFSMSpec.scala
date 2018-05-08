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
import vdx.stockpile.cli.CoreSpec.FileDeckLoader
import vdx.stockpile.cli.UISpec.WorkerFinished
import vdx.stockpile.instances.eq._
import vdx.stockpile.{CardList, Deck, Inventory}

import scala.concurrent.duration._

class CoreFSMSpec extends FlatSpec with Matchers {
  private type LoggedInventory[A] = Writer[Vector[InventoryLog], A]
  private type LoggedDeck[A] = Writer[Vector[DeckLog], A]

  implicit val system: ActorSystem = ActorSystem("test")

  def defaultLoader(inventory: Inventory = CardList.empty[InventoryCard]) =
    (f: File) => IO({ inventory.pure[LoggedInventory] })

  def defaultDeckLoader(decks: List[Deck[DeckListCard]] = List.empty) =
    new FileDeckLoader[DeckListCard] {
      var _decks = decks

      override def load(file: File): IO[DeckLoaderResult[DeckListCard]] = _decks match {
        case head :: tail =>
          _decks = tail
          head.pure[LoggedDeck].pure[IO]
        case _ => fail()
      }
    }

  def fsm(inventory: Inventory = CardList.empty[InventoryCard], decks: List[Deck[DeckListCard]] = List.empty) =
    TestFSMRef(
      new CoreFSM(
        defaultLoader(inventory),
        defaultDeckLoader(decks)
      )
    )

  "CoreFSM" should "start in Unintialised state" in {
    val core = fsm()

    core.stateName should be(CoreSpec.Uninitialized)
    core.stateData should be(CoreSpec.Empty)
  }

  def loadInventory() = {
    val inventory = CardList(InventoryCard("Path to Exile", 4, Edition("CON"), NonFoil))
    val core = fsm(inventory)

    core ! CoreSpec.LoadInventory(new File(""))
    (core, inventory)
  }

  it should "load the inventory when it receives the LoadInventory message and it is initialized" in {
    val (core, inventory) = loadInventory()

    core.stateData match {
      case CoreSpec.StateData(Some(i), _) => i should equal(inventory)
      case _                              => fail()
    }
  }

  it should "notify it's parent when the inventory is loaded" in {
    val parent = TestProbe()
    val logs = Vector(InventoryError("Warning 1"), InventoryError("Warning 2"))
    val core =
      parent.childActorOf(
        Props(
          classOf[CoreFSM],
          (f: File) => IO({ CardList.empty[InventoryCard].writer(logs) }),
          new FileDeckLoader[DeckListCard] {
            override def load(file: File): IO[DeckLoaderResult[DeckListCard]] = ???
          }
        )
      )
    parent.send(core, CoreSpec.LoadInventory(new File("")))

    parent.fishForMessage(1.second, "") {
      case UISpec.InventoryAvailable(_) => true
      case _                            => false
    } match {
      case UISpec.InventoryAvailable(l) => l should equal(logs)
      case _                            => fail()
    }
  }

  it should "change it state to InventoryLoaded after loading the inventory" in {
    val (core, _) = loadInventory()
    core.stateName should be(CoreSpec.InventoryLoaded)
  }

  it should "notify it's parent when a result is available" in {
    val parent = TestProbe()
    val core = parent.childActorOf(Props(classOf[CoreFSM], defaultLoader(), new FileDeckLoader[DeckListCard] {
      override def load(file: File): IO[DeckLoaderResult[DeckListCard]] = ???
    }))
    parent.send(core, CoreSpec.LoadInventory(new File("")))
    parent.send(core, CoreSpec.PrintInventory)

    parent.fishForMessage(1.second, "") {
      case WorkerFinished(UISpec.InventoryResult(i)) => true
      case _                                         => false
    } shouldBe an[WorkerFinished]
  }

  "CoreFSM :: LoadDecks" should "a single deck file" in {
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4))
    val core = fsm(
      CardList.empty,
      List(
        Deck(
          mainBoard = mainBoard
        )
      )
    )

    core ! CoreSpec.LoadInventory(new File(""))
    core ! CoreSpec.LoadDecks(new File(""))

    core.stateData match {
      case CoreSpec.StateData(_, decks: List[Deck[DeckListCard]]) => decks.head.mainBoard should equal(mainBoard)
      case _                                                      => fail()
    }
  }

  it should "notify it's parent when the decks are loaded" in {
    val parent = TestProbe()
    val mainBoard = CardList(DeckListCard("Tarmogoyf", 4), DeckListCard("Path to Exile", 4))
    val core = parent.childActorOf(
      Props(classOf[CoreFSM], defaultLoader(), defaultDeckLoader(List(Deck(mainBoard = mainBoard))))
    )

    parent.send(core, CoreSpec.LoadInventory(new File("")))
    core ! CoreSpec.LoadDecks(new File(""))

    parent.fishForMessage(1.second, "") {
      case WorkerFinished(UISpec.DecksAreLoaded) => true
      case _                                     => false
    } shouldBe an[WorkerFinished]
  }
}
