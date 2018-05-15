package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.DeckListCard

class PricerSpec extends FlatSpec with Matchers {
  private def service: IO[Client[IO]] = {
    val idProduct1 = 1337
    val idProduct2 = 1338
    val hs = HttpService[IO] {
      case GET -> Root / "ws" / "v2.0" / "output.json" / "products" / "find" =>
        Ok(
          ProductResult(
            List(
              Product(idProduct1, "Tarmogoyf", "Modern Masters 2017"),
              Product(idProduct2, "Tarmogoyf", "Modern Masters")
            ),
          ).asJson
        )
      case GET -> Root / "ws" / "v2.0" / "output.json" / "products" / pIdProduct =>
        pIdProduct.toInt match {
          case id if id == idProduct1 =>
            Ok(
              DetailedProductResult(
                DetailedProduct(
                  idProduct1,
                  "Tarmogoyf",
                  Expansion(1234, "Modern Masters 2017"),
                  PriceGuide(10, 10, 10, 10, 10, 9)
                )
              ).asJson
            )
          case id if id == idProduct2 =>
            Ok(
              DetailedProductResult(
                DetailedProduct(
                  idProduct2,
                  "Tarmogoyf",
                  Expansion(1234, "Modern Masters"),
                  PriceGuide(10, 10, 10, 10, 10, 12)
                )
              ).asJson
            )
        }
    }
    IO.pure(Client.fromHttpService(hs))
  }

  "Pricer" should "load the minimum price of the given card" in {
    implicit val httpService: IO[Client[IO]] = service

    val pricer = new Pricer
    val price = pricer.getPrice(DeckListCard("Tarmogoyf", 1)).unsafeRunSync()

    price.price.amount should be(9)
  }
}
