package vdx.stockpile.pricing.cardmarket

import org.http4s.Uri

case class APICredentials(
  appToken: String,
  appSecret: String,
  userToken: String,
  userSecret: String
)
case class CardmarketAPIConfig(
  baseUri: Uri,
  apiCredentials: APICredentials
)
