package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{HttpService, Request}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._

trait CardmarketSpec {
  val IdProduct1 = 1337
  val IdProduct2 = 1338

  def service(cb: Request[IO] => Unit = (_) => Unit): IO[Client[IO]] = {
    val hs = HttpService[IO] {
      case r @ GET -> Root / "ws" / "v2.0" / "output.json" / "products" / "find" =>
        cb(r)
        Ok(
          ProductResult(
            List(
              Product(IdProduct1, "Tarmogoyf", "Modern Masters 2017"),
              Product(IdProduct2, "Tarmogoyf", "Modern Masters")
            ),
          ).asJson
        )
      case r @ GET -> Root / "ws" / "v2.0" / "output.json" / "products" / pIdProduct =>
        cb(r)
        pIdProduct.toInt match {
          case id if id == IdProduct1 =>
            Ok(
              DetailedProductResult(
                DetailedProduct(
                  IdProduct1,
                  "Tarmogoyf",
                  Expansion(1234, "Modern Masters 2017"),
                  PriceGuide(10, 10, 10, 10, 10, 9)
                )
              ).asJson
            )
          case id if id == IdProduct2 =>
            Ok(
              DetailedProductResult(
                DetailedProduct(
                  IdProduct2,
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

}
