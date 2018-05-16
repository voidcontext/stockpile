package vdx.stockpile.pricing.cardmarket

import org.http4s.Uri
import org.scalatest.{FlatSpec, Matchers}

class APISpec extends FlatSpec with Matchers with Fetchables {

  override val config: CardmarketAPIConfig = CardmarketAPIConfig(
    Uri.unsafeFromString("http://example.com/api"),
    APICredentials(
      "foo",
      "bar",
      "baz",
      "foobar"
    )
  )

  "Product finder fetchable (by name)" should "be an implicit value" in {
    implicitly[Fetchable[List[Product], String]] shouldBe a[Fetchable[_, _]]
  }

  it should "define the correct endpoint" in {
    val request = productFetchable.request("product name").unsafeRunSync()
    request.uri.renderString should be(config.baseUri.renderString + "/products/find?search=product%20name&exact=true")
  }

  it should "accept the defined cardmarket payload" in {
    val product = Product(1, "Tarmogoyf", "Modern Masters")
    val productResult = ProductResult(List(product))

    productFetchable.extract(productResult) should equal(List(product))
  }

  it should "append the authorisation header to the request" in {
    val request = productFetchable.request("name").unsafeRunSync()

    request.headers.find(_.name.toString() == "Authorization").get.value should startWith(
      "OAuth realm=\"http://example.com/api/products/find\""
    )
  }

  "DetailedProduct fetchable (by Product)" should "be an implicit value" in {
    implicitly[Fetchable[DetailedProduct, Product]] shouldBe a[Fetchable[_, _]]
  }

  it should "define the correct endpoint" in {
    val product = Product(42, "Tarmogoyf", "Modern Masters")
    val request = detailedProductFetchable.request(product).unsafeRunSync()

    request.uri.renderString should be(config.baseUri.renderString + "/products/" + product.idProduct)
  }

  it should "accept the defined cardmarket payload" in {
    val detailedProduct =
      DetailedProduct(1, "Tarmogoyf", Expansion(1, "Modern Masters"), PriceGuide(10, 10, 10, 10, 10, 10))
    val detailedProductResult = DetailedProductResult(
      detailedProduct
    )

    detailedProductFetchable.extract(detailedProductResult) should equal(detailedProduct)
  }

  it should "append the authorisation header to the request" in {
    val request = detailedProductFetchable.request(Product(56, "Tarmogoyf", "Modern Masters")).unsafeRunSync()

    request.headers.find(_.name.toString() == "Authorization").get.value should startWith(
      "OAuth realm=\"http://example.com/api/products/56\""
    )
  }
}
