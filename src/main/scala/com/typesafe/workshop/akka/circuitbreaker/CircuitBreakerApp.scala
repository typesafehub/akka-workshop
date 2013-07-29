package com.typesafe.workshop.akka.circuitbreaker

import akka.actor._
import akka.actor.ActorDSL._
import akka.pattern._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Success, Failure }

object CircuitBreakerApp extends App {
  val system = ActorSystem("akka-patterns")

  val service = system.actorOf(Props[AccountService], name = "service")

  (1 to 5).foreach { x =>
    service ! GetAccountDetails(x.toString)
  }

  println()
  Console.readLine("Send second batch ?")
  println()

  (5 to 6).foreach { x =>
    service ! GetAccountDetails(x.toString)
  }

  println()

  Console.readLine("End it now?")
  println()

  system.shutdown()

}