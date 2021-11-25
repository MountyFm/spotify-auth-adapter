package kz.mounty.spotify.auth.adapter.util

import com.typesafe.config.Config

trait SpotifyUrlGetter {

  def config: Config

  private val spotifyBaseUrl = config.getString("spotify.base-url")

  protected def getSpotifyAuthUrl(clientId: String,
                                  encodedRedirectUri: String,
                                  scope: String): String =
    s"${spotifyBaseUrl}/authorize?client_id=$clientId&response_type=code&redirect_uri=$encodedRedirectUri&scope=$scope&show_dialog=false"

  protected def getSpotifyAccessTokenUrl(encodedRedirectUri: String,
                                         authToken: String,
                                         grantType: String): String =
    s"${spotifyBaseUrl}/api/token?grant_type=$grantType&code=$authToken&redirect_uri=$encodedRedirectUri"

  protected def getSpotifyRefreshedTokenUrl(grantType: String,
                                            refreshToken: String): String =

    s"${spotifyBaseUrl}/api/token?grant_type=$grantType&refresh_token=$refreshToken"
}
