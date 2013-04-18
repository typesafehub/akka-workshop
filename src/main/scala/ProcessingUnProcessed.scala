package akkapatterns

import akka.actor.{ Props, ActorSystem, Actor, ActorLogging }

object ProcessingUnProcessed extends App {

  class UnstableActor extends Actor with ActorLogging {

    def receive = {
      case s: String =>
        log.info("processing {}", s)
        if (s == "error") throw new Exception("Error")
        else sender ! "success"
    }
  }

  val system = ActorSystem("akka-patterns")

  val ref = system.actorOf(Props[UnstableActor])

  1 to 5 foreach (x => ref ! x.toString)

  ref ! "error"

  5 to 10 foreach (x => ref ! x.toString)

  Console.readLine("Enter to exit")

  system.shutdown()

}
