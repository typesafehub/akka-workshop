package com.typesafe.workshop.akka.patterns

import akka.actor._
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.Queue

object PullWorkerExample extends App {

  //messages
  case class WorkerJoining(workerRef: ActorRef)
  case class Work(payload: Any)
  case class WorkDone(result: Any)
  case class IAmIdle(worker: ActorRef)

  class Master extends Actor with ActorLogging {

    import scala.collection.mutable.{ Queue, ListBuffer }

    val workQ = Queue.empty[(ActorRef, Work)]
    val idleWorkers = Queue.empty[ActorRef]

    def receive = {
      case w: Work =>
        workQ.enqueue(sender -> w)
        if (!idleWorkers.isEmpty) sendWork(idleWorkers.dequeue)
      case WorkerJoining(worker) =>
        log.info("Adding new worker {}", worker)
        context.watch(worker)
        if (workQ.isEmpty) idleWorkers += worker
        else sendWork(worker)

      case IAmIdle(worker) =>
        if (workQ.isEmpty) idleWorkers.enqueue(worker)
        else sendWork(worker)

      case Terminated(worker) =>
        log.debug("Oops worker {} crashed", worker)
        idleWorkers.dequeueFirst(w => w == worker)
    }

    def sendWork(worker: ActorRef): Unit = {
      val (s, w) = workQ.dequeue
      worker.tell(w, s)
    }
  }

  class Worker(masterPath: String, workerId: Int) extends Actor with ActorLogging {

    var master: Option[ActorRef] = None

    override def preStart = {
      context.system.actorSelection(masterPath) ! Identify(workerId)
    }

    def receive = {
      case ActorIdentity(`workerId`, Some(ref)) =>
        master = Some(ref)
        master.foreach(_ ! WorkerJoining(self))
      case Work(payload) =>
        log.info("working on the playload {}", payload)
        if (payload.toString == "blow") throw new RuntimeException("Boom")
        log.info("Work is done by {}", self.path.name)
        sender ! WorkDone("Work is done by " + self.path.name)
        master.foreach(_ ! IAmIdle(self))
    }
  }

  val mainConfig = ConfigFactory.load

  val masterSystem = ActorSystem("master", mainConfig.getConfig("master"))
  val master = masterSystem.actorOf(Props[Master], name = "master")

  val remoteSystem1 = ActorSystem("remote1", mainConfig.getConfig("remote1"))
  val remoteSystem2 = ActorSystem("remote2", mainConfig.getConfig("remote2"))

  remoteSystem1.actorOf(Props(new Worker("akka.tcp://master@127.0.0.1:2553/user/master", 1)), name = "workerA")
  remoteSystem2.actorOf(Props(new Worker("akka.tcp://master@127.0.0.1:2553/user/master", 2)), name = "workerB")

  Console.readLine("Start?")

  master ! Work("Hey")
  master ! Work("Hey1")
  master ! Work("blow")
  master ! Work("Hey3")
  master ! Work("Hey4")
  master ! Work("Hey5")

  Console.readLine("Finish?")

  remoteSystem1.shutdown()
  remoteSystem2.shutdown()
  masterSystem.shutdown()

}
