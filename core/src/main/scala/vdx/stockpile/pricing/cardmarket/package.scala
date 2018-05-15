package vdx.stockpile.pricing

import cats.effect.IO
import io.circe.Decoder
import org.http4s.{EntityDecoder, Request}
import org.http4s.client.{Client => HttpClient}
import org.http4s.circe.jsonOf
import pureconfig._
import pureconfig.modules.http4s._

package object cardmarket {
  type RequestEnricher = Request[IO] => Request[IO]

  trait Fetchable[T, P] {
    type M

    def request(param: P): IO[Request[IO]]

    def entityDecoder(implicit de: Decoder[T]): EntityDecoder[IO, M]

    def extract(result: M): T
  }

  trait SimpleFetchable[T, P] extends Fetchable[T, P] {
    type M = T

    override def entityDecoder(implicit de: Decoder[T]): EntityDecoder[IO, T] = jsonOf[IO, T]

    override def extract(result: M): T = result
  }

  object Fetcher {
    def fetch[T, P](
      param: P
    )(implicit c: IO[HttpClient[IO]], fe: Fetchable[T, P], de: Decoder[T]): IO[T] = {
      c.flatMap {
        _.expect(fe.request(param))(fe.entityDecoder)
          .map(fe.extract)
      }
    }
  }

  implicit class FetcherOps[P](param: P) {
    def fetch[T](implicit c: IO[HttpClient[IO]], fe: Fetchable[T, P], de: Decoder[T]): IO[T] =
      Fetcher.fetch[T, P](param)
  }

  object fetchables extends Fetchables { // scalastyle:off object.name
    lazy val config: CMConfig =
      loadConfig[CMConfig](java.nio.file.Paths.get(System.getProperty("user.dir") + "/application.conf"), "cardmarket")
        .fold[CMConfig](err => throw new RuntimeException(err.head.description), config => config)
  }

  implicit val detailedProductOrdering = new Ordering[DetailedProduct] {
    override def compare(
      x: DetailedProduct,
      y: DetailedProduct
    ): Int =
      if (x.priceGuide.TREND == y.priceGuide.TREND) 0
      else if (x.priceGuide.TREND < y.priceGuide.TREND) -1
      else 1
  }
}
