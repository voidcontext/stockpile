package vdx.stockpile.pricing.cardmarket

import cats.effect.IO
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.client.dsl.io._
import org.scalatest.{FlatSpec, Matchers}

class FetchableSpec extends FlatSpec with Matchers {
  case class RemoteResource(id: Int, value: String)
  case class ComplexRemoteResource(resource: RemoteResource)

  val baseUri = Uri.unsafeFromString("http://example.com")
  val hs = HttpService[IO] {
    case GET -> Root / "remote-resource" / id =>
      Ok(RemoteResource(id.toInt, "foobar").asJson)
    case GET -> Root / "complex-remote-resource" / id =>
      Ok(ComplexRemoteResource(RemoteResource(id.toInt, "foobar")).asJson)
  }

  implicit val client: IO[Client[IO]] = IO.pure(Client.fromHttpService(hs))
  implicit object remoteResourceFetchable extends SimpleFetchable[RemoteResource, Int] {
    override def request(param: Int): IO[Request[IO]] = GET(baseUri / "remote-resource" / param.toString)
  }

  "Fetcher.fetch" should "fetch fetchables" in {
    val resource = Fetcher.fetch[RemoteResource, Int](1).unsafeRunSync()

    resource should equal(RemoteResource(1, "foobar"))
  }

  it should "fetch the resource from the given http client" in {
    var endpointCalled = false
    val hs = HttpService[IO] {
      case GET -> Root / "remote-resource" / id =>
        endpointCalled = true
        Ok(RemoteResource(id.toInt, "foobar").asJson)
    }

    implicit val client: IO[Client[IO]] = IO.pure(Client.fromHttpService(hs))

    Fetcher.fetch[RemoteResource, Int](1).unsafeRunSync()
    endpointCalled should be(true)
  }

  it should "fetch the given resource" in {
    val resourceId = 42
    val resource = Fetcher.fetch[RemoteResource, Int](resourceId).unsafeRunSync()

    resource should equal(RemoteResource(resourceId, "foobar"))
  }

  it should "extract requested object from the payload" in {
    implicit object remoteResourceFetchable extends Fetchable[RemoteResource, Int] {
      override type M = ComplexRemoteResource

      override def request(param: Int): IO[Request[IO]] = GET(baseUri / "complex-remote-resource" / param.toString)

      override def entityDecoder(
        implicit de: Decoder[RemoteResource]
      ): EntityDecoder[IO, M] = jsonOf[IO, M]

      override def extract(result: M): RemoteResource = result.resource
    }

    val resourceId = 42
    val resource = Fetcher.fetch[RemoteResource, Int](resourceId).unsafeRunSync()

    resource should equal(RemoteResource(resourceId, "foobar"))
  }

  "FetchOps" should "provide a fetch[T] extension method" in {
    val resource = 64.fetch[RemoteResource].unsafeRunSync()

    resource should equal(RemoteResource(64, "foobar"))
  }
}
