package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.{HttpService, Request}
import org.scalatest.{FlatSpec, Matchers}

class APISpec extends FlatSpec with Matchers {
  import fetchables._

  private def service(cb: Request[IO] => Unit = (_) => Unit): IO[Client[IO]] = {
    val hs = HttpService[IO] {
      case r @ GET -> Root / "ws" / "v2.0" / "output.json" / "products" / "find" =>
        cb(r)
        Ok(ProductResult(List(Product(1, "Tarmogoyf"))).asJson)
    }
    IO.pure(Client.fromHttpService(hs))
  }

  "Product" should "be fetchable" in {
    implicit val client = service()
    "Tarmogoyf".fetch[List[Product]].unsafeRunSync() shouldBe a[List[Product]]
  }

  "Fetchable Product" should "request the given name" in {
    var name = ""
    implicit val client = service((r) => name = r.params.getOrElse("search", ""))
    "Tarmogoyf".fetch[List[Product]].unsafeRunSync()

    name should be("Tarmogoyf")
  }

  it should "be processed and transformed" in {
    implicit val client = service()
    val products = "Nonexistent name".fetch[List[Product]].unsafeRunSync()

    products.head should be(Product(1, "Tarmogoyf"))
  }
}
