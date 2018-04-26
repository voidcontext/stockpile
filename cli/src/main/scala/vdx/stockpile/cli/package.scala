package vdx.stockpile

import java.io.File

package object cli {
  trait MenuItem {
    def key: Option[Char] = None
    def label: String
  }

  trait SubMenu extends MenuItem
  trait Action extends MenuItem

  def loadLastInventoryFromDownloads(): File = {
    def expand(path: String) = path.replaceFirst("^~", System.getenv("HOME"))

    new File(expand("~/Downloads"))
      .listFiles()
      .filter(_.getName.startsWith("Inventory"))
      .sortWith((a, b) => a.lastModified() > b.lastModified())
      .head
  }

}
