package util

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, `Content-Type`}
import akka.http.scaladsl.model.{headers, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.stream.Materializer
import com.typesafe.config.Config
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import kz.mounty.fm.exceptions.{ErrorCodes, ServerErrorRequestException}
import org.json4s.jackson.JsonMethods.parse
import scalaj.http.Base64

import java.security.cert.X509Certificate
import javax.net.ssl.{KeyManager, SSLContext, X509TrustManager}
import scala.concurrent.{ExecutionContext, Future, Promise}

trait HttpClient extends SerializersWithTypeHints with MountyEndpoint {
  def makePostRequest[T: Manifest](uri: String,
                                   headers: List[HttpHeader],
                                   body: String)
                                  (implicit system: ActorSystem,
                                   ec: ExecutionContext,
                                   mat: Materializer): Future[T] = {

    val badSSLConfig = AkkaSSLConfig().mapSettings(s => s.withLoose(s.loose.withDisableHostnameVerification(true)))
    val noCertificateCheckContext = ConnectionContext.https(trustfulSslContext, Some(badSSLConfig))

    val p = Promise[T]

    Http()
      .singleRequest(
        request = HttpRequest(
          uri = uri,
          method = HttpMethods.POST,
          headers = headers,
          entity = HttpEntity(
            ContentType(MediaTypes.`application/x-www-form-urlencoded`),
            body
          ),
          protocol = HttpProtocols.`HTTP/1.1`
        ),
        noCertificateCheckContext
      ).flatMap { httpResponse: HttpResponse =>
      httpResponse.status match {
        case StatusCodes.OK =>
          if (httpResponse.entity.contentType == ContentTypes.`application/json`) {
            Unmarshal(httpResponse.entity).to[String].map { jsonString =>
              p.success(parse(jsonString).camelizeKeys.extract[T])
            }
          }
        case _ =>
          p.failure(
            ServerErrorRequestException(
              ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
              Some(s"Http error response: ${httpResponse.status}")
            )
          )
      }
      p.future
    }
  }

  private val trustfulSslContext: SSLContext = {

    object NoCheckX509TrustManager extends X509TrustManager {
      override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}

      override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {}

      override def getAcceptedIssuers: Array[X509Certificate] = Array[X509Certificate]()
    }
    val context = SSLContext.getInstance("TLS")
    context.init(Array[KeyManager](), Array(NoCheckX509TrustManager), null)
    context
  }

  def getAuthorizationHeaders(implicit config: Config): List[HttpHeader] = {
    val clientId = config.getString("spotify.client-id")
    val clientSecret = config.getString("spotify.client-secret-id")
    val encoded = Base64.encodeString(s"$clientId:$clientSecret")

    List(
      `Content-Type`(ContentTypes.`application/x-www-form-urlencoded`),
      Authorization(headers.BasicHttpCredentials(s"${encoded}")),
    )
  }
}
