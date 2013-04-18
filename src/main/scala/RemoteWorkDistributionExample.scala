package akkapatterns

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.routing.FromConfig

object RemoteWorkDistributionExample extends App {

  class Master(worker: ActorRef) extends Actor with ActorLogging {

    def receive = {
      case e =>
        log.info("Distributing work {}", e)
        worker ! e
    }
  }

  class Worker extends Actor with ActorLogging {

    def receive = {
      case x =>
        log.info("received {} at {}", x, self)
    }
  }

  val mainConfig = ConfigFactory.load

  val masterSystem = ActorSystem("master", mainConfig.getConfig("master"))

  val worker = masterSystem.actorOf(Props[Worker].withRouter(FromConfig), name = "remote-worker")

  //This will be created in remote node
  val remoteSystem1 = ActorSystem("remote1", mainConfig.getConfig("remote1"))
  val remoteSystem2 = ActorSystem("remote2", mainConfig.getConfig("remote2"))

  val master = masterSystem.actorOf(Props(new Master(worker)), name = "master")

  1 to 5 foreach (master ! _)

  Console.readLine("Finish?")

  remoteSystem1.shutdown()
  remoteSystem2.shutdown()
  masterSystem.shutdown()

}
