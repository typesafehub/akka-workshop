package akkapatterns

import akka.actor._

import akka.pattern._
import scala.concurrent.duration._
import akka.util.Timeout

object GracefulStopExample extends App {

  class PingActor extends Actor with ActorLogging {

    def receive = {
      case m =>
        val deadLine = 1.second.fromNow
        while (deadLine.hasTimeLeft()) {}

        log.info("processing message {}", m)
        sender ! m
    }
  }

  val system = ActorSystem("akka-patterns")

  val ref = system.actorOf(Props[PingActor])

  1 to 5 foreach (ref ! _)

  import system.dispatcher
  implicit val timeout = Timeout(20.second)
  ref.ask(20).onSuccess {
    case r =>
      system.log.info("Waiting for all the messages to be processed")
      gracefulStop(ref, 10.second)(system).onSuccess { case _ => system.shutdown() }
  }

  30 to 35 foreach (ref ! _)

}
