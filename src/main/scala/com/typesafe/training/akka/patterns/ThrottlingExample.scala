package com.typesafe.training.akka.patterns

import akka.actor._
import com.typesafe.training.akka.common.ActorSystems._
import akka.actor.ActorDSL._
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._

object ThrottlingExample extends App {

  case object CheckCount

  class ProducerActor extends Actor with ActorLogging {

    val slowActor = context.actorOf(Props[SlowActor])
    val throttler = context.actorOf(Props(new Throttler(slowActor)).withDispatcher("akka.actor.throttler-dispatcher"), name = "throttler")
    def receive = {
      case x =>
        log.info("Forwarding message to")
        throttler forward x
    }
  }

  class Throttler(slowActor: ActorRef) extends Actor with ActorLogging with Stash {
    import context.dispatcher
    val messageInProgress: AtomicInteger = new AtomicInteger(0)
    def receive = {
      case x =>
        if (messageInProgress.get() > 5) {
          log.info("Lots of messages in progress. Stash messages from the next one")
          context.become({
            case CheckCount =>
              log.info("CheckCount event fired - {}", messageInProgress.get())
              if (messageInProgress.get() < 5) {
                log.info("Unstashing the messages")
                unstashAll()
                context.unbecome()
              } else {
                context.system.scheduler.scheduleOnce(5.seconds, self, CheckCount)
              }
            case x =>
              stash()
          }, false)
          context.system.scheduler.scheduleOnce(5.seconds, self, CheckCount)
        }
        val thisSender = sender
        slowActor.tell(x, context.actorOf(Props(new Act {
          become {
            case any =>
              log.info("Got the response for {}", any)
              thisSender ! any
              messageInProgress.decrementAndGet()
              context.stop(self)
          }
        })))
        messageInProgress.incrementAndGet()
    }
  }

  class SlowActor extends Actor with ActorLogging {
    def receive = {
      case any =>
        val deadline = 2.seconds.fromNow
        while (deadline.hasTimeLeft()) {}
        sender ! any
    }
  }

  val producer = applicationSystem.actorOf(Props[ProducerActor].withDispatcher("akka.actor.limited-dispatcher"))

  1 to 10 foreach (producer ! _)
  Console.readLine("finish?")

  shutdown()
}
