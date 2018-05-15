package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.Decoder
import io.circe.generic.auto._
import org.http4s.circe.jsonOf
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import org.http4s.{EntityDecoder, Header, Request}

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

  implicit object productFetchable extends Fetchable[List[Product], String] {
    override type M = ProductResult
    override def request(param: String): IO[Request[IO]] =
      GET(config.baseUri / "products" / "find" +? ("search", param) +? ("exact", "true")).enrich

    override def entityDecoder(implicit de: Decoder[List[Product]]): EntityDecoder[IO, ProductResult] =
      jsonOf[IO, ProductResult]

    override def extract(result: M): List[Product] = result.product
  }

  implicit object detailedProductFetchable extends Fetchable[DetailedProduct, Product] {
    override type M = DetailedProductResult
    override def request(param: Product): IO[Request[IO]] =
      GET(config.baseUri / "products" / param.idProduct.toString).enrich

    override def entityDecoder(implicit de: Decoder[DetailedProduct]): EntityDecoder[IO, DetailedProductResult] =
      jsonOf[IO, DetailedProductResult]

    override def extract(result: DetailedProductResult): DetailedProduct = result.product

  }

}
