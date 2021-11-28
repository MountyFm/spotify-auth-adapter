package kz.mounty.spotify.auth.adapter.domain

import kz.mounty.fm.domain.DomainEntity

case class GetSpotifyUserProfileRequest(accessToken: String) extends DomainEntity
