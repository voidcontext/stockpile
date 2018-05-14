package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import org.http4s.{EntityDecoder, Header, Request}
import org.http4s.circe.jsonOf
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._

trait Fetchables {
  def config: CMConfig

  implicit val enrichers: List[RequestEnricher] = List(
    (r: Request[IO]) => {
      r.withHeaders(
        r.headers.put(
          Header(
            "Authorization",
            OAuth1AuthorisationHeader
              .fromCredentials(
                config.apiCredentials,
                r.method.name,
                r.uri.renderString,
                ""
              )
              .content
          )
        )
      )
    }
  )

  implicit class RequestOps(reqIO: IO[Request[IO]]) {
    def enrich(implicit es: List[RequestEnricher]): IO[Request[IO]] = reqIO.map { req =>
      es.foldLeft(req)((req, enricher) => enricher(req))
    }
  }

  implicit val productFetchable: Fetchable[List[Product], String] =
    new Fetchable[List[Product], String] {
      override type M = ProductResult
      override def request(param: String): IO[Request[IO]] =
        GET(config.baseUri / "products" / "find" +? ("search", param) +? ("exact", "true")).enrich

      override def entityDecoder: EntityDecoder[IO, ProductResult] = jsonOf[IO, ProductResult]

      override def transform(result: M): List[Product] = result.product
    }
}
