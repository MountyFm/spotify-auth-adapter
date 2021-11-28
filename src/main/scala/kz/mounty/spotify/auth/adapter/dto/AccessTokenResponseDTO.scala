package kz.mounty.spotify.auth.adapter.dto

import kz.mounty.spotify.auth.adapter.domain.ApiResponse
import org.joda.time.DateTime

case class AccessTokenResponseDTO(tokenKey: String,
                                  refreshToken: Option[String] = None,
                                  expiresAfterDate: DateTime) extends ApiResponse
