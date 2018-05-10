package vdx.stockpile.cli

import java.io.File

import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import vdx.stockpile.Card.{DeckListCard, Edition, InventoryCard, NonFoil}
import vdx.stockpile.Inventory.InventoryError
import vdx.stockpile.cli.Core.DistinctHaves
import vdx.stockpile.cli.Menu.MenuItem
import vdx.stockpile.cli.UI.DecksAreLoaded
import vdx.stockpile.cli.console.Console
import vdx.stockpile.instances.eq._
import vdx.stockpile.{CardList, Inventory}

import scala.concurrent.Await
import scala.concurrent.duration._

class UISpec extends FlatSpec with Matchers with BeforeAndAfterAll {
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
    (probe, TestFSMRef(new UI(_ => probe.ref, console)))
  }

  def dummyHandler: PartialFunction[MenuItem, Unit] = {
    case _ => Unit
  }

  "UI" should "start in Uninitialized state" in {
    val (_, ui) = fsm()

    ui.stateName should be(UI.Uninitialized)
    ui.stateData should be(UI.Empty)
  }

  it should "delegate inventory loading to core once it's initialized" in {
    val (probe, ui) = fsm()
    probe.expectMsg(Core.LoadInventory(loadLastInventoryFromDownloads()))
  }

  it should "draw the main menu once the inventory is loaded to core" in {
    val console = new ConsoleMock(List(Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UI.InventoryAvailable(List.empty)

    ui.stateName should be(UI.InventoryOnlyScreen)
  }

  it should "process logs from the inventory load" in {
    val console = new ConsoleMock(List(Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)
    val logs = Vector(
      InventoryError("Warning 1"),
      InventoryError("Warning 2")
    )

    ui ! UI.InventoryAvailable(logs)

    console.printedLines should equal(logs.map(_.message))
  }

  it should "enter into Working state when running an action in a submenu" in {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.InventoryExportTerminal), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)

    ui.stateName should be(UI.Working)
  }

  private def printInventory(inventory: Inventory) = {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.InventoryExportTerminal, Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)

    ui.stateName should be(UI.Working)

    probe.fishForMessage(1.second, "") {
      case Core.PrintInventory => true
      case _                   => false
    } should be(Core.PrintInventory)

    ui ! UI.WorkerFinished(UI.InventoryResult(inventory))
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
    ui.stateName should be(UI.InventoryExportScreen)
  }

  it should "navigate back from submenu to the main menu when selecting Quit from the menu" in {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.Quit, Menu.Nop), List.empty)
    val (_, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)

    ui.stateName should be(UI.InventoryOnlyScreen)
  }

  it should "terminate the actor system when quitting from the main menu" in {
    implicit val systemToTerminate = ActorSystem("test-to-terminate")
    val probe = TestProbe()(systemToTerminate)
    val console = new ConsoleMock(List(Menu.Quit, Menu.Nop), List.empty)
    val (coreProbe, ui) = fsm(console)(systemToTerminate)

    probe.watch(ui)

    ui ! UI.InventoryAvailable(Vector.empty)

    probe.expectTerminated(ui)
    Await.ready(systemToTerminate.whenTerminated, 1.minutes)
  }

  "UI :: InventoryExport" should "enter into the selected submenu" in {
    val console = new ConsoleMock(List(Menu.InventoryExport, Menu.Nop), List.empty)
    val (probe, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)

    ui.stateName should be(UI.InventoryExportScreen)
    console.displayedMenus(1) should equal(Menu.export)
  }

  "UI :: LoadDecksFromDir" should "ask for the directory where to load the decks from" in {
    val console = new ConsoleMock(List(Menu.LoadDecksFromDir, Menu.Nop), List("decks"))
    val (_, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)

    console.inputs shouldBe empty
  }

  it should "hand over the work to the core" in {
    val console = new ConsoleMock(List(Menu.LoadDecksFromDir, Menu.Nop), List("decks"))
    val (coreProbe, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)

    coreProbe.fishForMessage(1.second, "") {
      case Core.LoadDecks(_) => true
      case _                 => false
    } should equal(Core.LoadDecks(new File("decks")))
  }

  it should "enter into working mode" in {
    val console = new ConsoleMock(List(Menu.LoadDecksFromDir, Menu.Nop), List("decks"))
    val (_, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)

    ui.stateName should be(UI.Working)
  }

  it should "enter into the selected submenu after the decks are loaded into core" in {
    val console = new ConsoleMock(List(Menu.LoadDecksFromDir, Menu.Nop), List("decks"))
    val (_, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)
    ui ! UI.WorkerFinished(DecksAreLoaded)

    ui.stateName should be(UI.DeckLoadedScreen)
  }

  it should "display the deck menu" in {
    val console = new ConsoleMock(List(Menu.LoadDecksFromDir, Menu.Nop), List("decks"))
    val (_, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)
    ui ! UI.WorkerFinished(DecksAreLoaded)
    console.displayedMenus(1) should equal(Menu.decks)
  }

  private def distinctHaves() = {
    val console = new ConsoleMock(List(Menu.LoadDecksFromDir, Menu.DistinctHaves, Menu.Nop), List("decks"))
    val (probe, ui) = fsm(console)

    ui ! UI.InventoryAvailable(Vector.empty)
    ui ! UI.WorkerFinished(DecksAreLoaded)

    (probe, ui, console)
  }

  "UI :: DistinctHaves" should "should delegate the work to the Core actor" in {
    val (probe, _, _) = distinctHaves()

    probe.fishForMessage(1.second) {
      case Core.DistinctHaves => true
      case _                  => false
    } should equal(Core.DistinctHaves)
  }

  it should "should put UI into working state" in {
    val (_, ui, _) = distinctHaves()
    ui.stateName should be(UI.Working)
  }

  it should "print the result" in {
    val (_, ui, console) = distinctHaves()

    val card = DeckListCard("Tarmogoyf", 4)
    ui ! UI.WorkerFinished(UI.DistinctHaves(List(CardList(card))))

    console.printedLines should equal(Vector(card.toString, ""))
  }
}
