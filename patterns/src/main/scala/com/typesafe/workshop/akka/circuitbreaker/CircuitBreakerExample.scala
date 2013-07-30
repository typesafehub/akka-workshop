package com.typesafe.workshop.akka.circuitbreaker

import akka.actor._
import akka.actor.ActorDSL._
import akka.pattern._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Success, Failure }

case class GetAccountDetails(accountId: String)
case class AccountDetails(details: Any)
case object SystemOverloaded

class AccountService(backendService: String => AccountDetails) extends Actor with ActorLogging {

  val remoteService = context.actorOf(Props(new RiskyActor(backendService)))
  def receive = {
    case a: GetAccountDetails =>
      log.info("Received get account details message")
      val thisSender = sender
      //do some stuff
      remoteService.tell(a, context.actorOf(Props(new Act with ActorLogging {
        become {
          case a: AccountDetails =>
            log.info("Sending the response to caller " + a)
            thisSender ! a

          case SystemOverloaded =>
            log.debug("System overloaded.Please try again latter")
            thisSender ! "Please try again later"
        }
      })))

  }
}

class RiskyActor(backendService: String => AccountDetails) extends Actor with ActorLogging {

  val config = context.system.settings.config
  val breaker =
    new CircuitBreaker(
      context.system.scheduler,
      maxFailures = config.getInt("circuitbreaker.max-failures"), 
      callTimeout = Duration(config.getMilliseconds("circuitbreaker.call-timeout"), MILLISECONDS), 
      resetTimeout = Duration(config.getMilliseconds("circuitbreaker.reset-timeout"), MILLISECONDS)
    ).onOpen(notifyMeOnOpen).onHalfOpen(notifyMeOnHalfOpen).onClose(notifyMeOnClose)

  def receive = {
    case GetAccountDetails(accountId) =>
      val thisSender = sender
      breaker.withCircuitBreaker(Future(invokeService(accountId))).onComplete {
        case Success(accountDetails) => thisSender ! accountDetails
        case Failure(e) =>
          log.error(e.toString)
          thisSender ! SystemOverloaded

      }
  }

  def notifyMeOnOpen() = {
    log.warning("Circuit breaker is now open")
  }

  def notifyMeOnClose() = {
    log.warning("Circuit breaker is now closed.")
  }

  def notifyMeOnHalfOpen() = {
    log.warning("Circuit breaker is now half open. Hopefully first request goes through successfully")
  }

  def invokeService(accountId: String) = {
    backendService(accountId)
  }
}

