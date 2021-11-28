package kz.mounty.spotify.auth.adapter.util

import kz.mounty.spotify.auth.adapter.domain.AccessTokenResponse
import kz.mounty.spotify.auth.adapter.dto.AccessTokenResponseDTO
import org.joda.time.DateTime

trait DTOConverter {
  def convert(accessTokenResponse: AccessTokenResponse, tokenKey: String): AccessTokenResponseDTO = {
    AccessTokenResponseDTO(
      tokenKey = tokenKey,
      refreshToken = accessTokenResponse.refreshToken,
      expiresIn = DateTime.now.plusSeconds(accessTokenResponse.expiresIn)
    )
  }
}
