package vdx.stockpile.pricing

import cats.effect.IO
import org.http4s.{EntityDecoder, Request}
import org.http4s.client.{Client => HttpClient}
import pureconfig._
import pureconfig.modules.http4s._

package object cardmarket {
  type RequestEnricher = Request[IO] => Request[IO]

  trait Fetchable[T, P] {
    type M

    def request(param: P): IO[Request[IO]]

    def entityDecoder: EntityDecoder[IO, M]

    def transform(result: M): T
  }

  object API {
    def fetch[T, P](
      param: P
    )(implicit c: IO[HttpClient[IO]], fe: Fetchable[T, P]): IO[T] = {
      c.flatMap { client =>
        client
          .expect(fe.request(param))(fe.entityDecoder)
          .map { entity =>
            fe.transform(entity)
          }
      }
    }
  }

  implicit class APIOps[P](param: P) {
    def fetch[T](implicit c: IO[HttpClient[IO]], fe: Fetchable[T, P]): IO[T] =
      API.fetch[T, P](param)
  }

  object fetchables extends Fetchables { // scalastyle:off object.name
    lazy val config: CMConfig =
      loadConfig[CMConfig](java.nio.file.Paths.get(System.getProperty("user.dir") + "/application.conf"), "cardmarket")
        .fold[CMConfig](err => throw new RuntimeException(err.head.description), config => config)
  }
}
