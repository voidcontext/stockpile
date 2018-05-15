package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.{HttpService, Request}
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.DeckListCard

class APISpec extends FlatSpec with Matchers {
  import fetchables._

  private def service(cb: Request[IO] => Unit = (_) => Unit): IO[Client[IO]] = {
    val hs = HttpService[IO] {
      case r @ GET -> Root / "ws" / "v2.0" / "output.json" / "products" / "find" =>
        cb(r)
        Ok(
          ProductResult(
            List(Product(1, "Tarmogoyf", "Modern Masters 2017"), Product(2, "Tarmogoyf", "Modern Masters"))
          ).asJson
        )
      case r @ GET -> Root / "ws" / "v2.0" / "output.json" / "products" / idProduct =>
        cb(r)
        Ok(
          DetailedProductResult(
            DetailedProduct(
              1,
              "Tarmogoyf",
              Expansion(1234, "Modern Masters 2017"),
              PriceGuide(10, 10, 10, 10, 10, 10)
            )
          ).asJson
        )
    }
    IO.pure(Client.fromHttpService(hs))
  }

  "Product" should "be fetchable" in {
    implicit val client: IO[Client[IO]] = service()
    "Tarmogoyf".fetch[List[Product]].unsafeRunSync() shouldBe a[List[Product]]
  }

  it should "be fetchable by name" in {
    var name = ""
    implicit val client: IO[Client[IO]] = service(r => name = r.params.getOrElse("search", ""))
    "Tarmogoyf".fetch[List[Product]].unsafeRunSync()

    name should be("Tarmogoyf")
  }

  it should "be processed and transformed" in {
    implicit val client: IO[Client[IO]] = service()
    val products = "Nonexistent name".fetch[List[Product]].unsafeRunSync()

    products.head should be(Product(1, "Tarmogoyf", "Modern Masters 2017"))
  }

  "DetailedProduct" should "be a fetchable" in {
    implicit val client: IO[Client[IO]] = service()
    Product(1, "Tarmogoyf", "Modern Masters 2017").fetch[DetailedProduct].unsafeRunSync() shouldBe a[DetailedProduct]
  }
}
