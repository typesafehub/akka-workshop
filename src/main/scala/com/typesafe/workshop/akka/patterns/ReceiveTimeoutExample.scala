package com.typesafe.workshop.akka.patterns

import akka.actor._

import scala.concurrent.duration._
import akka.actor.ActorDSL._

//process message within certain time otherwise fail

object ReceiveTimeoutExample extends App {

  case class Task(payload: Any)

  case class Success(r: Any)

  case object Failure

  class Distributor extends Actor with ActorLogging {

    val worker = context.actorOf(Props[SomeWorkerActor])
    val backupWorker = context.actorOf(Props[SomeWorkerActor])
    def receive = {
      case t: Task =>
        val listener = sender
        val responseTimeDetector = context.actorOf(Props(new ResponseTimeDetector(t, listener, backupWorker)))
        worker.tell(t, responseTimeDetector)

    }
  }

  class ResponseTimeDetector(m: Any, listener: ActorRef, workerRef: ActorRef) extends Actor with ActorLogging {

    context.setReceiveTimeout(1.seconds)

    val firstTry: Receive = {
      case s: Success =>
        log.info("received the response within the time")
        listener ! s
        context.stop(self)
      case ReceiveTimeout =>
        log.info("Did not receive in 1 second. Trying with backup with 2 seconds timeout")
        context.setReceiveTimeout(2.seconds)
        context.become(secondTry)
        workerRef.tell(m, self)
    }

    lazy val secondTry: Receive = {
      case s: Success =>
        log.info("Received response on second try")
        listener ! s
        context.stop(self)
      case ReceiveTimeout =>
        log.info("Did not receive message within 1 second")
        listener ! Failure
        context.stop(self)
    }

    def receive = firstTry

  }

  class SomeWorkerActor extends Actor with ActorLogging {
    import context.dispatcher
    def receive = {
      case Task(p) =>
        val actualSender = sender
        //do something important
        context.system.scheduler.scheduleOnce(3.seconds) {
          log.info("worker is done with the work")
          actualSender ! Success("i am done")
        }
    }
  }

  val system = ActorSystem("akka-patterns")

  val distributor = system.actorOf(Props[Distributor])

  system.actorOf(Props(new Act with ActorLogging {
    distributor ! Task("Do something")
    become {
      case s: Success =>
        log.info("It worked")
        context.system.shutdown()
      case Failure =>
        log.error("Some thing is messed up")
        context.system.shutdown()
    }
  }))

}
