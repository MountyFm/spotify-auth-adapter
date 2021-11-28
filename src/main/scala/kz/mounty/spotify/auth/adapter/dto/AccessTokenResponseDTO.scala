package kz.mounty.spotify.auth.adapter.dto

import org.joda.time.DateTime

case class AccessTokenResponseDTO(tokenKey: String,
                                  refreshToken: Option[String] = None,
                                  expiresIn: DateTime)
