package vdx.stockpile.pricing.cardmarket

trait BaseProduct {
  def idProduct: Int

  def enName: String
}

case class Product(idProduct: Int, enName: String, expansionName: String) extends BaseProduct

case class PriceGuide(SELL: Float, LOW: Float, LOWEX: Float, LOWFOIL: Float, AVG: Float, TREND: Float)
case class Expansion(idExpansion: Int, enName: String)
case class DetailedProduct(idProduct: Int, enName: String, expansion: Expansion, priceGuide: PriceGuide)
    extends BaseProduct

case class ProductResult(product: List[Product])
case class DetailedProductResult(product: DetailedProduct)
