package kz.mounty.spotify.auth.adapter.actors

import akka.actor.Props
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import kz.mounty.spotify.auth.adapter.domain.{ApiRequest, ApiResponse}
import kz.mounty.fm.exceptions.{ErrorCodes, MountyException, ServerErrorRequestException}
import kz.mounty.fm.serializers.Serializers
import org.json4s.jackson.Serialization
import org.json4s.Serialization
import kz.mounty.spotify.auth.adapter.util.{LoggerActor, MountyEndpoint}

import scala.concurrent.{ExecutionContext, Promise}

object PerRequest {

  class PerRequestActor(val request: ApiRequest,
                        val props: Props,
                        val promise: Promise[RouteResult],
                        val requestContext: RequestContext) extends PerRequest

}

trait PerRequest extends LoggerActor with Json4sSupport with MountyEndpoint with Serializers {
  implicit val serialization: Serialization = Serialization
  implicit val ex: ExecutionContext = context.dispatcher

  val request: ApiRequest
  val props: Props
  val promise: Promise[RouteResult]
  val requestContext: RequestContext

  context.actorOf(props) ! request

  override def receive: Receive = {
    case response: ApiResponse =>
      writeInfoLog("Received response", response)
      handleAndCompleteResponse(response)
    case e: MountyException =>
      writeErrorLog("Received error", e)
      handleAndCompleteErrorResponse(e)
    case any =>
      val exception = ServerErrorRequestException(
        ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
        Some(s"Received unhandled response: ${any}")
      )
      writeErrorLog("Received error", exception)
      handleAndCompleteErrorResponse(exception)
  }

  def handleAndCompleteResponse(response: ToResponseMarshallable): Unit = {
    requestContext
      .complete(response)
      .onComplete(response => promise.complete(response))

    context.stop(self)
  }

  def handleAndCompleteErrorResponse(error: MountyException): Unit = {
    handleAndCompleteResponse(error)
  }

  override def postStop(): Unit =
    log.warning("Stopped...")
}
