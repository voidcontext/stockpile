package vdx.stockpile.cli

case class Menu(actions: List[MenuItem]) {
  def asStrings: List[String] =
    indexed.map({
      case (action, index) => "[" + actionKey(action, index) + "] " + action.label
    })

  private def indexed = actions.zipWithIndex

  private def actionKey(action: MenuItem, index: Int): String = action.key.getOrElse(index + 1).toString

  def isValidKey(key: String): Boolean =
    indexed.map({ case (action, index) => actionKey(action, index) }).contains(key)

  def action(key: String): MenuItem =
    indexed.filter({ case (action, index) => actionKey(action, index) == key }).map(_._1).head
}

object Menu {
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

  val main = apply(
    List(
      InventoryExport,
      Quit,
    )
  )

  val export = apply(
    List(
      InventoryExportTerminal,
      Quit,
    )
  )
}
