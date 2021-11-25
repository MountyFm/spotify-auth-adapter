package kz.mounty.spotify.auth.adapter

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import kz.mounty.spotify.auth.adapter.rest.RestRouting
import kz.mounty.spotify.auth.adapter.util.{MountyEndpoint, SerializersWithTypeHints}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, SECONDS}

object Boot extends App with SerializersWithTypeHints with MountyEndpoint {
  implicit val config: Config = ConfigFactory.load()
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(FiniteDuration(config.getLong("request-timeout"), SECONDS))

  val host = config.getString("application.host")
  val port = config.getInt("application.port")

  val restRouting = new RestRouting()

  Http().bindAndHandle(restRouting.authRoute, host, port)

  system.log.info(s"running on $host:$port")
}
