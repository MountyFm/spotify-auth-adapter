package kz.mounty.spotify.auth.adapter.util

import kz.mounty.fm.exceptions.ErrorSeries

trait MountyEndpoint {
  implicit val errorSeries: ErrorSeries = ErrorSeries.SPOTIFY_AUTH_ADAPTER
}
