package vdx.stockpile.pricing.cardmarket

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import scala.util.Random

case class OAuth1AuthorisationHeader(content: String)

object OAuth1AuthorisationHeader {
  private val builder = new AuthorisationHeaderBuilder

  def fromCredentials(
    credentials: APICredentials,
    method: String,
    uri: String,
    body: String
  ): OAuth1AuthorisationHeader = {
    OAuth1AuthorisationHeader(
      builder.build(method, uri, body)(credentials)
    )
  }
}

// TODO: this class is coming from and old project. it needs serious refactoring
private[cardmarket] class AuthorisationHeaderBuilder() {

  private val sha1MessageDigest =
    java.security.MessageDigest.getInstance("SHA-1")

  def build(method: String, uri: String, body: String)(implicit oauthCredentials: APICredentials): String = {
    def urlEncode(string: String) =
      URLEncoder
        .encode(string, "UTF-8")
        .replaceAll("\\+", "%20")

    def splitQueryString(query: String) = {
      println(query)
      val kv = query match {
        case "" => List()
        case _ =>
          query
            .split('&')
            .map { kv: String =>
              println(kv)
              val fragments = kv.split("=").toList

              (fragments.head, fragments.tail.mkString("="))
            }
            .toList
      }

      println(kv)
      kv
    }

    val parsedUrl = com.netaporter.uri.Uri.parse(uri)

    val requestUrl = parsedUrl.removeAllParams().toString

    val realm = requestUrl
    val oauthVersion = "1.0"
    val oauthConsumerKey = oauthCredentials.appToken
    val oauthToken = oauthCredentials.userToken
    val oauthSignatureMethod = "HMAC-SHA1"
    val oauthTimestamp = (System.currentTimeMillis() / 1000).toString
    val oauthNonce = sha1MessageDigest
      .digest(
        (System.currentTimeMillis().toString + new Random().nextInt.toString)
          .getBytes("UTF-8")
      )
      .map("%02x".format(_))
      .mkString

    val encodedUrl = urlEncode(requestUrl)

    val queryParamTuples = parsedUrl.query.params.map { t: (String, Option[String]) =>
      (t._1, t._2.getOrElse("").replaceAll("\\+", "%20"))
    }

    val paramString = (
      queryParamTuples ++ //(splitQueryString(body)  map { t => (t._1, urlEncode(t._2)) }) ++
        (
          List(
            ("oauth_consumer_key", oauthConsumerKey),
            ("oauth_nonce", oauthNonce),
            ("oauth_signature_method", oauthSignatureMethod),
            ("oauth_timestamp", oauthTimestamp),
            ("oauth_token", oauthToken),
            ("oauth_version", oauthVersion)
          ).map { t =>
            (t._1, urlEncode(t._2))
          }
        )
    ).sortWith((a, b) => {
        a._1.compareTo(b._1) match {
          case 0          => a._2.compareTo(b._2) < 0
          case i if i < 0 => true
          case _          => false
        }
      })
      .map { t =>
        t._1 + "=" + urlEncode(t._2)
      }
      .mkString("&")

    //    println(paramString)

    val baseString = method + "&" + encodedUrl + "&" + urlEncode(paramString)

    // println(baseString)

    val signingKey = urlEncode(oauthCredentials.appSecret) + "&" + urlEncode(oauthCredentials.userSecret)

    val mac = Mac.getInstance("HmacSHA1")
    val secret = new SecretKeySpec(signingKey.getBytes, mac.getAlgorithm)
    mac.init(secret)

    val digest = mac.doFinal(baseString.getBytes)
    val encoder = java.util.Base64.getEncoder
    val oauthSignature = encoder.encodeToString(digest)

    "OAuth realm=\"" + realm + "\", " +
      "oauth_version=\"" + oauthVersion + "\", " +
      "oauth_timestamp=\"" + oauthTimestamp + "\", " +
      "oauth_nonce=\"" + oauthNonce + "\", " +
      "oauth_consumer_key=\"" + oauthConsumerKey + "\", " +
      "oauth_token=\"" + oauthToken + "\", " +
      "oauth_signature_method=\"" + oauthSignatureMethod + "\", " +
      "oauth_signature=\"" + oauthSignature + "\""

  }
}
