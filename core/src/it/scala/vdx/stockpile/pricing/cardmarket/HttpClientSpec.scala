package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.client.blaze.Http1Client
import org.scalatest.{FlatSpec, Matchers}

class HttpClientSpec extends FlatSpec with Matchers {
  import fetchables._

  "Fetcher :: fetchAll" should "result a succesful response" in {
    implicit val client = Http1Client[IO]()
    API.fetch[List[Product], String]("Tarmogoyf").unsafeRunSync() shouldBe a[List[_]]
  }
}
