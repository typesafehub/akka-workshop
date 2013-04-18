package akkapatterns

import com.typesafe.config.Config
import akka.actor._
import akka.dispatch._
import scala.concurrent.duration._
import java.util.concurrent.{ ConcurrentHashMap, ConcurrentLinkedQueue }
import java.util
import akka.agent.Agent
import scala.Some
import com.typesafe.config.ConfigFactory

object ActorSystems {
  val systemUtils = ActorSystem("system-utils", ConfigFactory.load.getConfig("akka.system-utils"))
  val applicationSystem = ActorSystem("akka-patterns")

  def shutdown() = {
    systemUtils.shutdown()
    applicationSystem.shutdown()
  }
}

import ActorSystems._

class TrackingMailboxType(systemSettings: ActorSystem.Settings, config: Config)
    extends MailboxType {

  override def create(owner: Option[ActorRef],
    system: Option[ActorSystem]): MessageQueue =
    (owner zip system) headOption match {
      case Some((o, s: ExtendedActorSystem)) ⇒
        val mailbox = new TrackingMailbox(s)
        TrackingMailboxExtension(s).register(o, mailbox)
        mailbox
      case _ ⇒
        throw new IllegalArgumentException("requires an owner " +
          "(i.e. does not work with BalancingDispatcher)")
    }
}

class TrackingMailbox(_system: ExtendedActorSystem) extends ConcurrentLinkedQueue[Envelope] with QueueBasedMessageQueue with UnboundedMessageQueueSemantics {

  private val tracker = Agent(0)(systemUtils)

  final def queue: util.Queue[Envelope] = this

  override def enqueue(receiver: ActorRef, envelope: Envelope): Unit = {
    systemUtils.log.info("enqueue message")
    tracker.send(_ + 1)
    super.enqueue(receiver, envelope)
  }

  override def dequeue(): Envelope = {
    systemUtils.log.info("dequeue message")
    tracker.send(_ - 1)
    super.dequeue()
  }

  //called when mailbox is disposed
  override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    tracker.close()
    TrackingMailboxExtension(_system).unregister(owner)
    super.cleanUp(owner, deadLetters)
  }

  def mailboxSize = tracker()
}

//Akka Extension is a mechanism to add functionality to Akka system
object TrackingMailboxExtension extends ExtensionId[TrackingboxExtension] with ExtensionIdProvider {
  def lookup = this
  def createExtension(s: ExtendedActorSystem) = new TrackingboxExtension(s)

  def mailboxSize()(implicit context: ActorContext): Unit = TrackingMailboxExtension(context.system).mailboxSize
}

class TrackingboxExtension(val system: ExtendedActorSystem) extends Extension {
  private val mailboxes = new ConcurrentHashMap[ActorRef, TrackingMailbox]

  def register(actorRef: ActorRef, mailbox: TrackingMailbox): Unit =
    mailboxes.put(actorRef, mailbox)

  def unregister(actorRef: ActorRef): Unit = mailboxes.remove(actorRef)

  def mailboxSize(implicit context: ActorContext): Int =
    mailboxes.get(context.self) match {
      case null ⇒ throw new IllegalArgumentException("Mailbox not registered for: " + context.self)
      case mailbox ⇒ mailbox.mailboxSize
    }
}

