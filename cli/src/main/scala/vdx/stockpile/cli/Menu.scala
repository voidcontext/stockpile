package vdx.stockpile.cli

import Menu._

case class Menu(actions: List[MenuItem]) {
  def asStrings: List[String] =
    indexed.map {
      case (action, index) => "[" + actionKey(action, index) + "] " + action.label
    }

  private def indexed = actions.zipWithIndex

  private def actionKey(action: MenuItem, index: Int): String = action.key.getOrElse(index + 1).toString

  def isValidKey(key: String): Boolean =
    indexed
      .map { case (action, index) => actionKey(action, index) }
      .contains(key)

  def action(key: String): MenuItem =
    indexed.collect { case (action, index) if actionKey(action, index) == key => action }.head
}

object Menu {
  trait MenuItem {
    def key: Option[Char] = None
    def label: String
  }

  trait SubMenu extends MenuItem
  trait Action extends MenuItem

  case object Quit extends MenuItem {
    override def key: Option[Char] = Option('q')
    override def label: String = "Quit"
  }

  case object Nop extends MenuItem {
    override def label: String = "Nop"
  }

  case object InventoryExport extends MenuItem { override def label: String = "Export" }
  case object InventoryExportTerminal extends Action {
    override def label: String = "Print to screen"
  }

  case object LoadDecksFromDir extends MenuItem { override def label: String = "Load Decks from directory" }
  case object DistinctHaves extends MenuItem { override def label: String = "Haves (sharing cards)" }
  case object DistinctMissing extends MenuItem { override def label: String = "Missing (sharing cards)" }
  case object PriceDistinctMissing extends MenuItem { override def label: String = "Price" }

  val main = apply(
    List(
      InventoryExport,
      LoadDecksFromDir,
      Quit
    )
  )

  val export = apply(
    List(
      InventoryExportTerminal,
      Quit
    )
  )

  val decks = apply(
    List(
      DistinctHaves,
      DistinctMissing,
      PriceDistinctMissing,
      Quit
    )
  )

}
