package vdx.stockpile.pricing

import cats.effect.IO
import io.circe.Decoder
import org.http4s.{EntityDecoder, Request}
import org.http4s.client.{Client => HttpClient}
import org.http4s.circe.jsonOf
import pureconfig._
import pureconfig.modules.http4s._
import vdx.stockpile.Card

package object cardmarket {
  type RequestEnricher = Request[IO] => Request[IO]

  /**
   * Specification of a remote resource
   */
  trait Fetchable[T, P] {
    type M

    def request(param: P): IO[Request[IO]]

    def entityDecoder(implicit de: Decoder[T]): EntityDecoder[IO, M]

    def extract(result: M): T
  }

  /**
   * Specification of a simple remote resource. A simple remote resource is the root entity of the response payload.
   */
  trait SimpleFetchable[T, P] extends Fetchable[T, P] {
    type M = T

    override def entityDecoder(implicit de: Decoder[T]): EntityDecoder[IO, T] = jsonOf[IO, T]

    override def extract(result: M): T = result
  }

  object Fetcher {

    /**
     * This function is fetching a remote resource using the corresponding Fetchable specification and transforms it
     * into the appropriate case class.
     */
    def fetch[T, P](
      param: P
    )(implicit c: IO[HttpClient[IO]], fe: Fetchable[T, P], de: Decoder[T]): IO[T] = {
      c.flatMap {
        _.expect(fe.request(param))(fe.entityDecoder)
          .map(fe.extract)
      }
    }
  }

  /**
   * Syntactic sugar to fetch remote resources based on the input parameter and the result type.
   */
  implicit class FetcherOps[P](param: P) {
    def fetch[T](implicit c: IO[HttpClient[IO]], fe: Fetchable[T, P], de: Decoder[T]): IO[T] =
      Fetcher.fetch[T, P](param)
  }

  /**
   * Fetchable implicits with a default configuration
   */
  object fetchables extends Fetchables { // scalastyle:off object.name
    lazy val config: CardmarketAPIConfig =
      loadConfig[CardmarketAPIConfig](
        java.nio.file.Paths.get(System.getProperty("user.dir") + "/application.conf"),
        "cardmarket"
      ).fold[CardmarketAPIConfig](err => throw new RuntimeException(err.head.description), config => config)
  }

  implicit val detailedProductOrdering: Ordering[DetailedProduct] = (x: DetailedProduct, y: DetailedProduct) =>
    if (x.priceGuide.TREND == y.priceGuide.TREND) 0
    else if (x.priceGuide.TREND < y.priceGuide.TREND) -1
    else 1

  implicit class DetailedProductOps(detailedProduct: DetailedProduct) {
    def toCardPrice[A <: Card[A]](card: A) = CardPrice(card, Price(detailedProduct.priceGuide.TREND, EUR, Cardmarket))
  }
}
