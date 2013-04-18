package akkapatterns

import akka.actor._
import akka.routing.RoundRobinRouter

object RouterExample extends App {

  class Worker(i: Int) extends Actor with ActorLogging {

    override def preRestart(reason: Throwable, message: Option[Any]) = {
      log.info("Worker {} is about to get restarted", i)
    }

    override def postRestart(reason: Throwable) = {
      log.info("Worker {} is restarted", i)
    }
    def receive = {
      case x =>
        if (i == 9) throw new RuntimeException("fglfjg")
        log.info("actor {} is processing {}", i, x)
        sender ! x
    }

  }

  val system = ActorSystem("akka-patterns")

  val workers = 1 to 10 map (x => system.actorOf(Props(new Worker(x))))

  val ref = system.actorOf(Props[Worker].withRouter(RoundRobinRouter(routees = workers)))

  1 to 20 foreach (x => ref ! x)

  Console.readLine("finish?")

  system.shutdown()
}
