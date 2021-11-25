package kz.mounty.spotify.auth.adapter.rest

import kz.mounty.spotify.auth.adapter.actors.SpotifyServiceActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives.{entity, _}
import com.typesafe.config.Config
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import kz.mounty.spotify.auth.adapter.domain.{GenerateAccessTokenRequest, GenerateSpotifyAuthUrlRequest, RefreshAccessTokenRequest}
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats, Serialization}

class RestRouting()(implicit val timeout: Timeout,
                    config: Config,
                    system: ActorSystem,
                    ex: ExecutionContext) extends BaseRoute with Json4sSupport {

  val spotifyServiceActorProps: Props = SpotifyServiceActor.props

  implicit val formats: Formats = DefaultFormats
  implicit val serialization: Serialization = Serialization

  val authRoute: Route = pathPrefix("spotify-auth-adapter") {
    concat(
      authGetRoute,
      authPostRoute
    )
  }

  private def authGetRoute: Route =
    get {
      concat(
        path("auth-url") {
          pathEndOrSingleSlash {
            parameters('redirectUri.as[String].?) { redirectUri =>
              handleApiRequestWithProps(
                spotifyServiceActorProps,
                SpotifyServiceActor.GenerateSpotifyAuthUrl(
                  body = GenerateSpotifyAuthUrlRequest(
                    clientId = config.getString("spotify.client-id"),
                    redirectUri = redirectUri.getOrElse(""),
                    scope = config.getString("spotify.scope")
                  )
                )
              )
            }
          }
        },
      )
    }

  private def authPostRoute: Route =
    post {
      concat(
        path("access-token") {
          entity(as[GenerateAccessTokenRequest]) { entity =>
            handleApiRequestWithProps(
              spotifyServiceActorProps,
              SpotifyServiceActor.GenerateAccessToken(
                body = entity
              )
            )
          }
        },
        path("refresh-access-token") {
          entity(as[RefreshAccessTokenRequest]) { entity =>
            handleApiRequestWithProps(
              spotifyServiceActorProps,
              SpotifyServiceActor.RefreshAccessToken(
                body = entity
              )
            )
          }
        }
      )
    }
}