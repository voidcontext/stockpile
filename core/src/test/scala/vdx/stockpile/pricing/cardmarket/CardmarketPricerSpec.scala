package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import org.http4s.client.Client
import org.scalatest.{FlatSpec, Matchers}
import vdx.stockpile.Card.DeckListCard

class CardmarketPricerSpec extends FlatSpec with CardmarketSpec with Matchers {

  "Pricer" should "load the minimum price of the given card" in {
    implicit val httpService: IO[Client[IO]] = service()

    val pricer = new Pricer
    val price = pricer.getPrice(DeckListCard("Tarmogoyf", 1)).unsafeRunSync()

    price.price.amount should be(9)
  }
}
