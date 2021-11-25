package kz.mounty.spotify.auth.adapter.domain

case class GenerateSpotifyAuthUrlRequest(clientId: String,
                                         redirectUri: String,
                                         scope: String) extends ApiRequest

case class GenerateSpotifyAuthUrlResponse(url: String) extends ApiResponse