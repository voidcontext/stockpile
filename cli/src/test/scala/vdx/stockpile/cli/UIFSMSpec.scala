package vdx.stockpile.cli

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{TestFSMRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import vdx.stockpile.cli.console.Console

class UIFSMSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("test")

  class ConsoleMock[State](var actions: List[MenuItem], var inputs: List[String]) extends Console {

    var displayedMenus = Vector[Menu]()

    override def menu[A](menu: Menu)(selectedHandler: MenuItem => A): A = actions match {
      case head :: tail =>
        displayedMenus = displayedMenus :+ menu
        actions = tail
        selectedHandler(head)
    }

    override def readLine(prompt: String): String = inputs match {
      case head :: tail =>
        inputs = tail
        head
    }
  }

  private def fsm() = {
    TestFSMRef(new UIFSM)
  }

  def dummyHandler: PartialFunction[MenuItem, Unit] = {
    case _ => Unit
  }

  "UIFSM" should "start in Uninitialized state" in {
    val ui = fsm()

    ui.stateName should be(UISpec.Uninitialized)
    ui.stateData should be(UISpec.Empty)
  }

  it should "setup its context after initialisation" in {
    val ui = fsm()

    ui ! UISpec.Initialize(dummyHandler, new ConsoleMock(List.empty, List.empty))

    ui.stateName should be(UISpec.Initialized)
    ui.stateData match {
      case UISpec.Context(_, _, stack) => stack shouldBe empty
    }
  }

  it should "draw the main menu once the inventory is loaded to core" in {
    val ui = fsm()

    ui ! UISpec.Initialize(dummyHandler, new ConsoleMock(List(Menu.Nop), List.empty))
    ui ! UISpec.InventoryAvailable

    ui.stateName should be(UISpec.InventoryOnlyScreen)
  }

  it should "enter into the 'Export' submenu when it is selected" in {
    val ui = fsm()

    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.Nop), List.empty)

    ui ! UISpec.Initialize(dummyHandler, console)
    ui ! UISpec.InventoryAvailable

    ui.stateName should be(UISpec.InventoryExportScreen)
    console.displayedMenus(1) should equal(Menu.export)
  }

  it should "enter into Working state when running an action in a submenu" in {
    val ui = fsm()

    ui ! UISpec.Initialize(
      dummyHandler,
      new ConsoleMock(List(Menu.InventoryExport, Menu.InventoryExportTerminal, Menu.Nop), List.empty)
    )
    ui ! UISpec.InventoryAvailable

    ui.stateName should be(UISpec.Working)
  }
}
