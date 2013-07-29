package com.typesafe.training.extra

import akka.actor._
import akka.actor.ActorDSL._

object TracingMessagesExample extends App {

  sealed trait Message {
    val id: Long
  }

  trait WireTrap extends Actor with ActorLogging {

    def before: Receive = {
      case e: Message =>
        //use some event sourcing to collect the result
        log.debug("Message id: {} is just received by {} at {}", e.id, self, System.currentTimeMillis())
    }

    abstract override def receive = {
      case m =>
        before(m)
        super.receive(m)
        after(m)
    }

    def after: Receive = {
      case e: Message =>
        //use some event sourcing to collect the result
        log.debug("Message id: {} is just processed by {} at {}", e.id, self, System.currentTimeMillis())
    }
  }

  case class SampleMessage(id: Long, message: String) extends Message

  class DelegateActor(next: ActorRef) extends Actor {

    override def receive = {
      case m => next forward m
    }
  }

  val system = ActorSystem("akka-patterns")

  val ref = system.actorOf(Props(new Act with ActorLogging {
    become {
      case m => log.info("Got the same message back {} ", m)
    }
  }))

  val actorD = system.actorOf(Props(new DelegateActor(ref) with WireTrap), name = "actorD")
  val actorC = system.actorOf(Props(new DelegateActor(actorD) with WireTrap), name = "actorC")
  val actorB = system.actorOf(Props(new DelegateActor(actorC) with WireTrap), name = "actorB")
  val actorA = system.actorOf(Props(new DelegateActor(actorB) with WireTrap), name = "actorA")

  actorA ! SampleMessage(10, "Hey")

  Console.readLine("Finish?")

  system.shutdown()

}
