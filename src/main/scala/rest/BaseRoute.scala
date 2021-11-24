package rest

import actors.PerRequest.PerRequestActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.server.{Route, RouteResult}
import domain.ApiRequest

import scala.concurrent.Promise

trait BaseRoute {
  def handleApiRequestWithProps(props: Props,
                                body: ApiRequest)(implicit system: ActorSystem): Route =
    ctx => {
      val promise = Promise[RouteResult]
      system.actorOf(Props(new PerRequestActor(body, props, promise, ctx)))
      promise.future
    }
}
