package akkapatterns

import akka.actor._
import akkapatterns.ActorSystems._
import scala.concurrent.duration._
import akka.routing.{ Broadcast, RoundRobinRouter }

object BecomeUnbecomeExample extends App {

  class BusyActor(canBecome: Boolean = false) extends Actor with ActorLogging {

    override def postStop(): Unit = {
      log.info("Stopping actor name {}", self.path.name)
    }

    val defaultBehavior: Receive = {
      case x =>
        val deadline = 2.second.fromNow
        while (deadline.hasTimeLeft()) {}
        sender ! x
        if (TrackingMailboxExtension(context.system).mailboxSize > 5 && canBecome) {
          log.info("Too many messages. Become an router")
          val ref = context.system.actorOf(Props(new BusyActor).withRouter(RoundRobinRouter(nrOfInstances = 5)).withDispatcher("akka.actor.custom-dispatcher"))
          context.become(routingBehavior(ref), false)
        }
    }

    def routingBehavior(router: ActorRef): Receive = {
      case x =>
        router forward x
        log.debug("Forwarding message {}", x)
        if (TrackingMailboxExtension(context.system).mailboxSize < 5 && canBecome) {
          log.info("Not many messages to process...unbecome")
          //when all the routees die router also dies
          router ! Broadcast(PoisonPill)
          context.unbecome()
        }
    }

    def receive = defaultBehavior
  }

  val ref = applicationSystem.actorOf(Props(new BusyActor(true)).withDispatcher("akka.actor.custom-dispatcher"), name = "Main")

  (1 to 30) foreach (ref ! _)

  Console.readLine("Finish?")

  ActorSystems.shutdown()

}
