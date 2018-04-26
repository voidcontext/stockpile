package vdx.stockpile.cli

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestFSMRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import vdx.stockpile.Card.{Edition, InventoryCard, NonFoil}
import vdx.stockpile.{CardList, Inventory}
import vdx.stockpile.cli.console.Console
import vdx.stockpile.instances.eq._

import scala.concurrent.duration._
import scala.concurrent.Await

class UIFSMSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("test")

  class ConsoleMock[State](var actions: List[MenuItem], var inputs: List[String]) extends Console {

    var displayedMenus: Vector[Menu] = Vector()
    var printedLines: Vector[String] = Vector()

    override def menu[A](menu: Menu)(selectedHandler: MenuItem => A): A = actions match {
      case head :: tail =>
        displayedMenus = displayedMenus :+ menu
        actions = tail
        selectedHandler(head)
      case _ => fail()
    }

    override def readLine(prompt: String): String = inputs match {
      case head :: tail =>
        inputs = tail
        head
      case _ => fail()
    }

    override def println(x: Any): Unit = printedLines = printedLines :+ x.toString
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
      case _                           => fail()
    }
  }

  it should "draw the main menu once the inventory is loaded to core" in {
    val ui = fsm()

    ui ! UISpec.Initialize(dummyHandler, new ConsoleMock(List(Menu.Nop), List.empty))
    ui ! UISpec.InventoryAvailable

    ui.stateName should be(UISpec.InventoryOnlyScreen)
  }

  it should "enter into the selected submenu" in {
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
      new ConsoleMock(List(Menu.InventoryExport, Menu.InventoryExportTerminal), List.empty)
    )
    ui ! UISpec.InventoryAvailable

    ui.stateName should be(UISpec.Working)
  }

  def printInventory(ui: TestFSMRef[UISpec.State, UISpec.Data, UIFSM], inventory: Inventory) = {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.InventoryExportTerminal, Menu.Nop), List.empty)

    ui ! UISpec.Initialize(dummyHandler, console)
    ui ! UISpec.InventoryAvailable

    ui.stateName should be(UISpec.Working)

    ui ! UISpec.WorkerFinished(UISpec.InventoryResult(inventory))
    console
  }

  it should "receive a result when it's in Working state" in {
    val ui = fsm()
    val inventory = CardList(
      InventoryCard("Path To Exile", 4, Edition("CON"), NonFoil),
      InventoryCard("Tarmogoyf", 4, Edition("MM3"), NonFoil)
    )

    val console = printInventory(ui, inventory)

    console.printedLines should equal(inventory.toList.map(_.toString))
  }

  it should "go back to the previous screen after processing the result" in {
    val ui = fsm()
    val inventory = CardList(
      InventoryCard("Path To Exile", 4, Edition("CON"), NonFoil),
      InventoryCard("Tarmogoyf", 4, Edition("MM3"), NonFoil)
    )

    printInventory(ui, inventory)
    ui.stateName should be(UISpec.InventoryExportScreen)
  }

  it should "navigate back from submenu to the main menu when selecting Quit from the menu" in {
    val ui = fsm()
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.Quit, Menu.Nop), List.empty)

    ui ! UISpec.Initialize(dummyHandler, console)
    ui ! UISpec.InventoryAvailable

    ui.stateName should be(UISpec.InventoryOnlyScreen)
  }

  it should "terminate the actor system when quitting from the main menu" in {
    val probe = TestProbe()
    val ui = fsm()

    probe.watch(ui)
    val console = new ConsoleMock(List(Menu.Quit, Menu.Nop), List.empty)

    ui ! UISpec.Initialize(dummyHandler, console)
    ui ! UISpec.InventoryAvailable

    probe.expectTerminated(ui)
    Await.ready(system.whenTerminated, 1.minutes)
  }
}
