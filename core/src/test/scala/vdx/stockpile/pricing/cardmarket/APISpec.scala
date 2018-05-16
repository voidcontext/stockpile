package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.client.Client
import org.scalatest.{FlatSpec, Matchers}

class APISpec extends FlatSpec with CardmarketSpec with Matchers {
  import fetchables._

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

    products.head should be(Product(IdProduct1, "Tarmogoyf", "Modern Masters 2017"))
  }

  "DetailedProduct" should "be a fetchable" in {
    implicit val client: IO[Client[IO]] = service()
    Product(IdProduct1, "Tarmogoyf", "Modern Masters 2017")
      .fetch[DetailedProduct]
      .unsafeRunSync() shouldBe a[DetailedProduct]
  }
}
