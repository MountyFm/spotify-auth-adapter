package kz.mounty.spotify.auth.adapter.actors

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.typesafe.config.Config
import kz.mounty.spotify.auth.adapter.util.{DTOConverter, RestClient, LoggerActor, SpotifyUrlGetter}

import java.net.URLEncoder
import scala.concurrent.ExecutionContext
import kz.mounty.spotify.auth.adapter.domain._
import kz.mounty.fm.exceptions.{ErrorCodes, ServerErrorRequestException}
import scredis.Redis

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

object SpotifyServiceActor {
  def props(redis: Redis)(implicit timeout: Timeout,
                          config: Config,
                          system: ActorSystem,
                          executionContext: ExecutionContext): Props = Props(new SpotifyServiceActor(redis))

  trait Message[T] extends ApiRequest {
    val body: T
  }

  sealed trait Request[T] extends Message[T]

  sealed trait Command[T] extends Message[T]

  case class GenerateSpotifyAuthUrl(body: GenerateSpotifyAuthUrlRequest) extends Command[GenerateSpotifyAuthUrlRequest] {
    val description = "Generating spotify auth url"
  }

  case class GenerateAccessToken(body: GenerateAccessTokenRequest) extends Command[GenerateAccessTokenRequest] {
    val description = "Generating Spotify API access token"
  }

  case class RefreshAccessToken(body: RefreshAccessTokenRequest) extends Command[RefreshAccessTokenRequest] {
    val description = "Refreshing Spotify API access token"
  }

}

class SpotifyServiceActor(redis: Redis)(implicit timeout: Timeout,
                                        override val config: Config,
                                        system: ActorSystem,
                                        executionContext: ExecutionContext) extends LoggerActor
  with RestClient
  with SpotifyUrlGetter
  with DTOConverter {

  override def receive: Receive = {
    case command: SpotifyServiceActor.GenerateSpotifyAuthUrl =>
      writeInfoLog(command.description, command.body)
      val encodedRedirectUri = URLEncoder.encode(command.body.redirectUri, "UTF-8")

      val url = getSpotifyAuthUrl(
        command.body.clientId,
        encodedRedirectUri,
        command.body.scope
      )
      context.parent ! GenerateSpotifyAuthUrlResponse(url = url)

    case command: SpotifyServiceActor.GenerateAccessToken =>
      writeInfoLog(command.description, command.body)

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
        case res: AccessTokenResponse =>
          putAccessTokenToRedisAndSendResponse(res)
        case any =>
          val exception = ServerErrorRequestException(
            ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
            Some(s"Received unhandled response while generating token: $any")
          )
          context.parent ! exception
      } recover {
        case NonFatal(e) =>
          val exception = ServerErrorRequestException(
            ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
            Some(s"Received exception while generating access token: ${e.getMessage}")
          )
          context.parent ! exception
      }

    case command: SpotifyServiceActor.RefreshAccessToken =>
      writeInfoLog(command.description, command.body)

      val url = getSpotifyRefreshedTokenUrl(
        grantType = config.getString("spotify.refresh-grant-type"),
        refreshToken = command.body.refreshToken
      )
      makePostRequest[AccessTokenResponse](
        uri = url,
        headers = getAuthorizationHeaders,
        body = ""
      ).map {
        case res: AccessTokenResponse =>
          putAccessTokenToRedisAndSendResponse(res)
        case any =>
          val exception = ServerErrorRequestException(
            ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
            Some(s"Received unhandled response while generating token: $any")
          )
          context.parent ! exception
      }
  }

  def putAccessTokenToRedisAndSendResponse(res: AccessTokenResponse): Unit = {
    val tokenKey = UUID.randomUUID().toString
    redis.set(tokenKey, res.accessToken, Some(FiniteDuration(res.expiresIn, TimeUnit.SECONDS))).onComplete {
      case Success(isWritten) if (isWritten) =>
        context.parent ! convert(res, tokenKey)
      case Success(isWritten) if (!isWritten) =>
        val exception = ServerErrorRequestException(
          ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
          Some(s"access token was not written to redis")
        )
        context.parent ! exception
      case Failure(exception) =>
        context.parent ! exception
    }
  }
}
