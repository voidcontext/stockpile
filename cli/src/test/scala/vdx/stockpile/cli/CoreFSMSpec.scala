package vdx.stockpile.cli

import java.io.File

import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestProbe}
import cats.data.Writer
import cats.effect.IO
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.InventoryCard
import vdx.stockpile.CardList
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
}
