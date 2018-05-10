package vdx.stockpile.cli

import vdx.stockpile.cli.Menu.MenuItem

package object console {

  trait Console {
    def menu[A](menu: Menu)(selectedHandler: MenuItem => A): A

    def readLine(prompt: String = ""): String

    def println(x: Any = ""): Unit
  }

  class Terminal extends Console {
    override def menu[A](menu: Menu)(selectedHandler: MenuItem => A): A = {
      menu.asStrings.foreach(println)
      var actionKey = readLine("Select: ")
      while (!menu.isValidKey(actionKey)) {
        actionKey = readLine("Select: ")
      }

      selectedHandler(menu.action(actionKey))
    }

    override def readLine(prompt: String = ""): String = scala.io.StdIn.readLine(prompt)

    override def println(x: Any = ""): Unit = scala.Predef.println(x)
  }
}
