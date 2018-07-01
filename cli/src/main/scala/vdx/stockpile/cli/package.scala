package vdx.stockpile

import java.io.File

package object cli {

  def loadLastInventoryFromDownloads(): File = {
    def expand(path: String) = path.replaceFirst("^~", System.getenv("HOME"))

    new File(expand("~/Downloads"))
      .listFiles()
      .filter(_.getName.startsWith("Inventory"))
      .sortWith((a, b) => a.lastModified() > b.lastModified())
      .head
  }

  trait Extractor[F[_]] {
    def extract[A](fa: F[A]): A
  }

  object Extractor {
    object syntax {
      implicit class ExtractorOps[A, F[_]](fa: F[A]) {
        def extract(implicit ex: Extractor[F]): A = ex.extract[A](fa)
      }
    }
  }
}
