package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.client.blaze.Http1Client
import org.scalatest.{FlatSpec, Matchers}

class HttpClientSpec extends FlatSpec with Matchers {
  import fetchables._

  "API :: fetch[List[Product], String]" should "find products by name" in {
    implicit val client = Http1Client[IO]()
    Fetcher.fetch[List[Product], String]("Tarmogoyf").unsafeRunSync() shouldBe a[List[_]]
  }

  "API :: fetch[DetailedProduct, Product]" should "load a DetailedProduct resource" in {
    implicit val client = Http1Client[IO]()
    val product = Fetcher
      .fetch[List[Product], String]("Tarmogoyf")
      .map(_.filter(_.expansionName == "Modern Masters 2017"))
      .unsafeRunSync()
      .head

    val detailedProduct = Fetcher.fetch[DetailedProduct, Product](product).unsafeRunSync()

    detailedProduct shouldBe a[DetailedProduct]
  }
}
