package com.typesafe.training.akka.patterns

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberJoined
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.Cluster

object ScaleOutWithAkkaCluster extends App {

  val mainConfig = ConfigFactory.load

  if (args.nonEmpty) System.setProperty("akka.remote.netty.port", args(0))

  //println(">>>>>>>>> config " + mainConfig.getConfig("ClusterSystem"))
  val system = ActorSystem("cs")

  val clusterListener = system.actorOf(Props(new Actor with ActorLogging {
    def receive = {
      case state: CurrentClusterState ⇒
        log.info("Current members: {}", state.members)
      case MemberJoined(member) ⇒
        log.info("Member joined: {}", member)

        println("Member joined " + member)
      case MemberUp(member) ⇒
        log.info("Member is Up: {}", member)

        println("Member up " + member)
      case UnreachableMember(member) ⇒
        log.info("Member detected as unreachable: {}", member)

        println("unreachable Member " + member)
      case _: ClusterDomainEvent ⇒ // ignore

    }
  }), name = "clusterListener")

  Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])
}