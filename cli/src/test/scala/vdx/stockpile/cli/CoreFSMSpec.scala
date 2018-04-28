package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestFSMRef, TestProbe}
import cats.data.Writer
import cats.effect.IO
import cats.implicits._
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.{Edition, InventoryCard, NonFoil}
import vdx.stockpile.Inventory.{InventoryError, InventoryLog}
import vdx.stockpile.cli.UISpec.WorkerFinished
import vdx.stockpile.{CardList, Inventory}

import scala.concurrent.duration._

class CoreFSMSpec extends FlatSpec with Matchers {
  private type Logged[A] = Writer[Vector[InventoryLog], A]

  implicit val system: ActorSystem = ActorSystem("test")

  def defaultLoader(inventory: Inventory = CardList.empty[InventoryCard]) =
    (f: File) => IO({ inventory.pure[Logged] })

  def fsm(inventory: Inventory = CardList.empty[InventoryCard]) =
    TestFSMRef(new CoreFSM(defaultLoader(inventory)))

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
      case CoreSpec.StateData(Some(i)) => i should equal(inventory)
      case _                           => fail()
    }
  }

  it should "notify it's parent when the inventory is loaded" in {
    val parent = TestProbe()
    val logs = Vector(InventoryError("Warning 1"), InventoryError("Warning 2"))
    val core =
      parent.childActorOf(Props(classOf[CoreFSM], (f: File) => IO({ CardList.empty[InventoryCard].writer(logs) })))
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
    val core = parent.childActorOf(Props(classOf[CoreFSM], defaultLoader()))
    parent.send(core, CoreSpec.LoadInventory(new File("")))
    parent.send(core, CoreSpec.PrintInventory)

    parent.fishForMessage(1.second, "") {
      case WorkerFinished(UISpec.InventoryResult(i)) => true
      case _                                         => false
    } shouldBe an[WorkerFinished]
  }
}
