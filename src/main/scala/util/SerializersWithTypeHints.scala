package util

import domain.{AccessTokenResponse, GenerateAccessTokenRequest, GenerateSpotifyAuthUrlRequest, GenerateSpotifyAuthUrlResponse, RefreshAccessTokenRequest}
import kz.mounty.fm.exceptions.ExceptionInfo
import org.json4s.ShortTypeHints
import org.json4s.jackson.Serialization

trait SerializersWithTypeHints {

  implicit val formats =
    Serialization.formats(
      ShortTypeHints(
        List(
          classOf[GenerateAccessTokenRequest],
          classOf[RefreshAccessTokenRequest],
          classOf[AccessTokenResponse],
          classOf[GenerateSpotifyAuthUrlRequest],
          classOf[GenerateSpotifyAuthUrlResponse],
          classOf[ExceptionInfo]
        )
      )
    )
}