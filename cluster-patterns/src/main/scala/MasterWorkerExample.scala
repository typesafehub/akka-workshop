package akkapatterns

import language.postfixOps
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.RootActorPath
import akka.actor.Terminated
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

object messages {
  case class TransformationJob(text: String)
  case class TransformationResult(text: String)
  case class JobFailed(reason: String, job: TransformationJob)
  case object WorkerRegistration
}

import messages._
class Worker extends Actor {
  val cluster = Cluster(context.system)
  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case TransformationJob(text) ⇒ sender ! TransformationResult(text.toUpperCase)
    case state: CurrentClusterState ⇒
      state.members.filter(_.status == MemberStatus.Up) foreach register
    case MemberUp(m) ⇒ register(m)
  }

  def register(member: Member): Unit =
    if (member.hasRole("master")) {
      context.actorSelection(RootActorPath(member.address) / "user" / "master") !
        WorkerRegistration      
    }
}

object Worker extends App {
  val config =
    (if (args.nonEmpty) ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${args(0)}")
    else ConfigFactory.empty).withFallback(
      ConfigFactory.parseString("akka.cluster.roles = [worker]")).
      withFallback(ConfigFactory.load())

  val system = ActorSystem("ClusterSystem", config)
  val worker = system.actorOf(Props(new Worker), name = "worker")

}

class Master extends Actor {
  var backends = IndexedSeq.empty[ActorRef]
  var jobCounter = 0
  def receive = {
    case job: TransformationJob if backends.isEmpty ⇒
      sender ! JobFailed("Service unavailable, try again later", job)
    case job: TransformationJob ⇒
      jobCounter += 1
      backends(jobCounter % backends.size) forward job
    case WorkerRegistration if !backends.contains(sender) ⇒
      context watch sender
      backends = backends :+ sender
    case Terminated(a) ⇒
      backends = backends.filterNot(_ == a)
  }
}

object Master extends App {
  val config =
    (if (args.nonEmpty) ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${args(0)}")
    else ConfigFactory.empty).withFallback(
      ConfigFactory.parseString("akka.cluster.roles = [master]")).
      withFallback(ConfigFactory.load())

  val system = ActorSystem("ClusterSystem", config)
  val master = system.actorOf(Props(new Master), name = "master")

  import system.dispatcher
  implicit val timeout = Timeout(5 seconds)
  for (n ← 1 to 120) {
    (master ? TransformationJob("hello-" + n)) onSuccess {
      case result ⇒ println(result)
    }
    // wait a while until next request,
    // to avoid flooding the console with output
    Thread.sleep(2000)
  }
  system.shutdown()
}