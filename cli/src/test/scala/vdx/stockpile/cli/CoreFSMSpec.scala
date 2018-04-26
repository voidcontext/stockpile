package vdx.stockpile.cli

import java.io.File

import akka.actor.ActorSystem
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

  def fsm() = TestFSMRef(new CoreFSM)

  "CoreFSM" should "start in Unintialised state" in {
    val core = fsm()

    core.stateName should be(CoreSpec.Uninitialized)
    core.stateData should be(CoreSpec.Empty)
  }

  it should "store ui ref and inventory loader after initialisation" in {
    val core = fsm()
    val probe = TestProbe()

    core ! CoreSpec.Initialize(probe.ref, (f: File) => IO({ CardList.empty[InventoryCard].pure[Logged] }))

    core.stateName should be(CoreSpec.Initialized)
  }

  it should "load the inventory when it recieves the LoadInventory message and it is initialized" in {
    val core = fsm()
    val probe = TestProbe()
    val inventory = CardList(InventoryCard("Path to Exile", 4, Edition("CON"), NonFoil))

    core ! CoreSpec.Initialize(
      probe.ref,
      (f: File) =>
        IO({
          inventory.pure[Logged]
        })
    )
    core ! CoreSpec.LoadInventory(new File(""))

    core.stateName should be(CoreSpec.InventoryLoaded)
    core.stateData match {
      case CoreSpec.Context(_, _, Some(i)) => i should equal(inventory)
      case _                               => fail()
    }
  }
}
