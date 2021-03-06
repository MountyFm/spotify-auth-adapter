package kz.mounty.spotify.auth.adapter.domain

case class GenerateAccessTokenRequest(authToken: String,
                                      redirectUri: String) extends ApiRequest

case class RefreshAccessTokenRequest(refreshToken: String) extends ApiRequest

case class AccessTokenResponse(accessToken: String,
                               tokenType: String,
                               expiresIn: Int,
                               refreshToken: Option[String],
                               scope: String)
