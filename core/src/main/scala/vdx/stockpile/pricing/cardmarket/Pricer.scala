package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import cats.implicits._
import io.circe.generic.auto._
import org.http4s.client.Client
import vdx.stockpile.Card.DeckListCard
import vdx.stockpile.pricing._
import vdx.stockpile.pricing.cardmarket.fetchables._
import vdx.stockpile.{Card, pricing}

class Pricer(implicit c: IO[Client[IO]]) extends PricerAlg[IO] {
  override def getPrice[A <: Card[A]](card: A): IO[pricing.CardPrice[A]] = card match {
    case DeckListCard(name, _) =>
      name
        .fetch[List[Product]]
        .flatMap[List[DetailedProduct]] { list =>
          list.parTraverse { product =>
            product.fetch[DetailedProduct]
          }
        }
        .map(_.min)
        .map { product =>
          CardPrice(
            card,
            Price(
              product.priceGuide.TREND,
              EUR,
              Cardmarket
            )
          )
        }

  }
}
