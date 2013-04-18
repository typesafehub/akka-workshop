package akkapatterns

import akka.actor._
import akka.actor.ActorDSL._
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

object CircuitBreakerExample extends App {

  case class GetAccountDetails(accountId: String)
  case class AccountDetails(details: Any)
  case object SystemOverloaded

  class AccountService extends Actor with ActorLogging {

    val remoteService = context.actorOf(Props[RemoteServiceActor])
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

  class RemoteServiceActor extends Actor with ActorLogging {

    val breaker =
      new CircuitBreaker(context.system.scheduler,
        maxFailures = 2,
        callTimeout = 2.seconds,
        resetTimeout = 1.minute).onOpen(notifyMeOnOpen).onHalfOpen(notifyMeOnHalfOpen)

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
      log.warning("Circuit breaker is now open for 1 minute")
    }

    def notifyMeOnHalfOpen() = {
      log.warning("Circuit breaker is now half open. Hopefully first request goes through successfully")
    }

    def invokeService(accountId: String) = {
      val deadline = 3.second.fromNow
      while (deadline.hasTimeLeft()) {}

      AccountDetails("You are broke")
    }
  }

  val system = ActorSystem("akka-patterns")

  val service = system.actorOf(Props[AccountService], name = "service")

  (1 to 5).foreach { x =>
    service ! GetAccountDetails(x.toString)
  }

  println()
  Console.readLine("Send second batch ?")
  println()

  (5 to 6).foreach { x =>
    service ! GetAccountDetails(x.toString)
  }

  println()

  Console.readLine("End it now?")
  println()

  system.shutdown()

}
