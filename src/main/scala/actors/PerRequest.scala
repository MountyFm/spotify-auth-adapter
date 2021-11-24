package actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import domain.{ApiRequest, ApiResponse}
import kz.mounty.fm.exceptions.{ErrorCodes, MountyException, ServerErrorRequestException}
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats, Serialization}
import util.MountyEndpoint

import scala.concurrent.{ExecutionContext, Promise}

object PerRequest {

  class PerRequestActor(val request: ApiRequest,
                        val props: Props,
                        val promise: Promise[RouteResult],
                        val requestContext: RequestContext) extends PerRequest

}

trait PerRequest extends Actor with ActorLogging with Json4sSupport with MountyEndpoint {
  implicit val formats: Formats = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val ex: ExecutionContext = context.dispatcher

  val request: ApiRequest
  val props: Props
  val promise: Promise[RouteResult]
  val requestContext: RequestContext

  context.actorOf(props) ! request

  override def receive: Receive = {
    case response: ApiResponse =>
      handleAndCompleteResponse(response)
    case e: MountyException =>
      handleAndCompleteErrorResponse(e)
    case any =>
      val exception = ServerErrorRequestException(
        ErrorCodes.INTERNAL_SERVER_ERROR(errorSeries),
        Some(s"Received unhandled response: ${any}")
      )
      handleAndCompleteErrorResponse(exception)
  }

  def handleAndCompleteResponse(response: ToResponseMarshallable): Unit = {
    requestContext
      .complete(response)
      .onComplete(response => promise.complete(response))

    context.stop(self)
  }

  def handleAndCompleteErrorResponse(error: MountyException): Unit = {
    val exceptionInfo = error.getExceptionInfo
    handleAndCompleteResponse(exceptionInfo)
  }

  override def postStop(): Unit =
    log.warning("Stopped...")
}
