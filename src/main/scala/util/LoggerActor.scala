package util

import akka.actor.{Actor, ActorLogging}

trait LoggerActor extends Actor with ActorLogging {
  def writeInfoLog[T](message: String,
                      entity: T): Unit = {
    log.info(s"[$message]: $entity")
  }

  def writeErrorLog[T](errorMessage: String,
                       entity: T): Unit = {
    log.error(s"[$errorMessage]: $entity")
  }
}
