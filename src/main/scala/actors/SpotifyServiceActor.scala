package actors

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import com.typesafe.config.Config
import util.{HttpClient, SpotifyUrlGetter}

import java.net.URLEncoder
import scala.concurrent.ExecutionContext
import domain._
import kz.mounty.fm.exceptions.{ErrorCodes, ServerErrorRequestException}

import scala.util.control.NonFatal

object SpotifyServiceActor {
  def props(implicit timeout: Timeout,
            config: Config,
            system: ActorSystem,
            executionContext: ExecutionContext): Props = Props(new SpotifyServiceActor())

  trait Message[T] extends ApiRequest {
    val body: T
  }

  sealed trait Request[T] extends Message[T]

  sealed trait Command[T] extends Message[T]

  case class GenerateSpotifyAuthUrl(body: GenerateSpotifyAuthUrlRequest) extends Command[GenerateSpotifyAuthUrlRequest]

  case class GenerateAccessToken(body: GenerateAccessTokenRequest) extends Command[GenerateAccessTokenRequest]

  case class RefreshAccessToken(body: RefreshAccessTokenRequest) extends Command[RefreshAccessTokenRequest]

}

class SpotifyServiceActor(implicit timeout: Timeout,
                          override val config: Config,
                          system: ActorSystem,
                          executionContext: ExecutionContext) extends Actor with HttpClient with SpotifyUrlGetter {

  override def receive: Receive = {
    case command: SpotifyServiceActor.GenerateSpotifyAuthUrl =>
      val encodedRedirectUri = URLEncoder.encode(command.body.redirectUri, "UTF-8")

      val url = getSpotifyAuthUrl(
        command.body.clientId,
        encodedRedirectUri,
        command.body.scope
      )

      context.parent ! GenerateSpotifyAuthUrlResponse(url = url)

    case command: SpotifyServiceActor.GenerateAccessToken =>
      val encodedRedirectUri = URLEncoder.encode(command.body.redirectUri, "UTF-8")

      val url = getSpotifyAccessTokenUrl(
        encodedRedirectUri = encodedRedirectUri,
        authToken = command.body.authToken,
        grantType = config.getString("spotify.auth-grant-type"))

      makePostRequest[AccessTokenResponse](
        uri = url,
        headers = getAuthorizationHeaders,
        body = ""
      ).map {
        res =>
          context.parent ! res
      } recover {
        case NonFatal(e) =>
          val exception = ServerErrorRequestException(
            ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
            Some(s"Received exception while generating access token: ${e.getMessage}")
          )
          context.parent ! exception
      }

    case command: SpotifyServiceActor.RefreshAccessToken =>
      val url = getSpotifyRefreshedTokenUrl(
        grantType = config.getString("spotify.refresh-grant-type"),
        refreshToken = command.body.refreshToken
      )

      makePostRequest[AccessTokenResponse](
        uri = url,
        headers = getAuthorizationHeaders,
        body = ""
      ).map {
        res =>
          context.parent ! res
      } recover {
        case NonFatal(e) =>
          val exception = ServerErrorRequestException(
            ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
            Some(s"Received exception while generating access token: ${e.getMessage}")
          )
          context.parent ! exception
      }
  }
}
