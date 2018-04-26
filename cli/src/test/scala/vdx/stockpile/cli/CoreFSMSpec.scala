package vdx.stockpile.cli

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestFSMRef, TestProbe}
import cats.data.Writer
import cats.effect.IO
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.{Edition, InventoryCard, NonFoil}
import vdx.stockpile.{CardList, Inventory}
import vdx.stockpile.Inventory.InventoryLoaderLog
import cats.implicits._

class CoreFSMSpec extends FlatSpec with Matchers {
  private type Logged[A] = Writer[Vector[InventoryLoaderLog], A]

  implicit val system: ActorSystem = ActorSystem("test")

  def fsm(inventory: Inventory = CardList.empty[InventoryCard]) =
    TestFSMRef(new CoreFSM((f: File) => IO({ inventory.pure[Logged] })))

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
      case CoreSpec.Context(Some(i)) => i should equal(inventory)
      case _                         => fail()
    }
  }

  it should "change it state to InventoryLoaded after loading the inventory" in {
    val (core, _) = loadInventory()
    core.stateName should be(CoreSpec.InventoryLoaded)
  }

  ignore should "notify it's parent when the inventory is laoded" in {}
}
