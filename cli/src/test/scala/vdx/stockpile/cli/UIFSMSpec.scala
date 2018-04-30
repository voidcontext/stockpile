package vdx.stockpile.cli

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{TestFSMRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import vdx.stockpile.Card.{Edition, InventoryCard, NonFoil}
import vdx.stockpile.Inventory.InventoryError
import vdx.stockpile.{CardList, Inventory}
import vdx.stockpile.cli.console.Console
import vdx.stockpile.instances.eq._

import scala.concurrent.duration._
import scala.concurrent.Await

class UIFSMSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("test")

  class ConsoleMock(var actions: List[MenuItem] = List.empty, var inputs: List[String] = List.empty) extends Console {

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

  private def fsm(console: Console = new ConsoleMock())(implicit system: ActorSystem) = {
    val probe = TestProbe()
    (probe, TestFSMRef(new UIFSM(_ => probe.ref, console)))
  }

  def dummyHandler: PartialFunction[MenuItem, Unit] = {
    case _ => Unit
  }

  "UIFSM" should "start in Uninitialized state" in {
    val (_, ui) = fsm()

    ui.stateName should be(UISpec.Uninitialized)
    ui.stateData should be(UISpec.Empty)
  }

  it should "delegate inventory loading to core once it's initialized" in {
    val (probe, ui) = fsm()
    probe.expectMsg(CoreSpec.LoadInventory(loadLastInventoryFromDownloads()))
  }

  it should "draw the main menu once the inventory is loaded to core" in {
    val console = new ConsoleMock(List(Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UISpec.InventoryAvailable(List.empty)

    ui.stateName should be(UISpec.InventoryOnlyScreen)
  }

  it should "process logs from the inventory load" in {
    val console = new ConsoleMock(List(Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)
    val logs = Vector(
      InventoryError("Warning 1"),
      InventoryError("Warning 2")
    )

    ui ! UISpec.InventoryAvailable(logs)

    console.printedLines should equal(logs.map(_.message))
  }

  it should "enter into Working state when running an action in a submenu" in {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.InventoryExportTerminal), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UISpec.InventoryAvailable(Vector.empty)

    ui.stateName should be(UISpec.Working)
  }

  def printInventory(inventory: Inventory) = {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.InventoryExportTerminal, Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UISpec.InventoryAvailable(Vector.empty)

    ui.stateName should be(UISpec.Working)

    probe.fishForMessage(1.second, "") {
      case CoreSpec.PrintInventory => true
      case _                       => false
    } should be(CoreSpec.PrintInventory)

    ui ! UISpec.WorkerFinished(UISpec.InventoryResult(inventory))
    (probe, ui, console)
  }

  it should "receive a result when it's in Working state" in {
    val inventory = CardList(
      InventoryCard("Path To Exile", 4, Edition("CON"), NonFoil),
      InventoryCard("Tarmogoyf", 4, Edition("MM3"), NonFoil)
    )

    val (_, ui, console) = printInventory(inventory)

    console.printedLines should equal(inventory.toList.map(_.toString))
  }

  it should "go back to the previous screen after processing the result" in {
    val inventory = CardList(
      InventoryCard("Path To Exile", 4, Edition("CON"), NonFoil),
      InventoryCard("Tarmogoyf", 4, Edition("MM3"), NonFoil)
    )

    val (_, ui, _) = printInventory(inventory)
    ui.stateName should be(UISpec.InventoryExportScreen)
  }

  it should "navigate back from submenu to the main menu when selecting Quit from the menu" in {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.Quit, Menu.Nop), List.empty)
    val (_, ui) = fsm(console)

    ui ! UISpec.InventoryAvailable(Vector.empty)

    ui.stateName should be(UISpec.InventoryOnlyScreen)
  }

  it should "terminate the actor system when quitting from the main menu" in {
    implicit val systemToTerminate = ActorSystem("test-to-terminate")
    val probe = TestProbe()(systemToTerminate)
    val console = new ConsoleMock(List(Menu.Quit, Menu.Nop), List.empty)
    val (coreProbe, ui) = fsm(console)(systemToTerminate)

    probe.watch(ui)

    ui ! UISpec.InventoryAvailable(Vector.empty)

    probe.expectTerminated(ui)
    Await.ready(systemToTerminate.whenTerminated, 1.minutes)
  }

  "UIFSM :: InventoryExport" should "enter into the selected submenu" in {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UISpec.InventoryAvailable(Vector.empty)

    ui.stateName should be(UISpec.InventoryExportScreen)
    console.displayedMenus(1) should equal(Menu.export)
  }

  "UIFSM :: LoadDecksFromDir" should "enter into the selected submenu" in {
    val console = new ConsoleMock(List(Menu.LoadDecksFromDir, Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UISpec.InventoryAvailable(Vector.empty)

    ui.stateName should be(UISpec.DeckLoadedScreen)
  }

  it should "display the deck menu" in {
    val console = new ConsoleMock(List(Menu.LoadDecksFromDir, Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UISpec.InventoryAvailable(Vector.empty)
    console.displayedMenus(1) should equal(Menu.decks)
  }

}
