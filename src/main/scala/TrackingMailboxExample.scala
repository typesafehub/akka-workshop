package akkapatterns

import akka.actor._
import scala.concurrent.duration._

import ActorSystems._

object TrackingMailboxExample extends App {

  class SomeActor extends Actor with ActorLogging {

    def receive = {
      case e =>
        val delayer = 2.second.fromNow
        while (delayer.hasTimeLeft()) {}
        log.debug("Messages in my mailbox " + TrackingMailboxExtension(context.system).mailboxSize)
    }
  }

  val ref = applicationSystem.actorOf(Props[SomeActor].withDispatcher("akka.actor.custom-dispatcher"))
  1 to 20 foreach (ref ! _)

  Console.readLine("finish?")

  systemUtils.shutdown()
  applicationSystem.shutdown()

}
