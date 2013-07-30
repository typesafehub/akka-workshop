package com.typesafe.workshop.akka.circuitbreaker

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.testkit.ImplicitSender
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.Props
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.testkit.EventFilter

class CircuitBreakerSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpec with MustMatchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("testSystem", ConfigFactory.parseString("""
        circuitbreaker.max-failures = 2	
        circuitbreaker.call-timeout = 200
        circuitbreaker.reset-timeout = 2000
      
        akka.loggers = ["akka.testkit.TestEventListener"]        
      """)))

  override def afterAll {
    TestKit.shutdownActorSystem(_system)
  }

  val testBackend: String => AccountDetails = {
    case "slowId" =>
      //simulate error
      val deadline = 2.seconds.fromNow
      while (deadline.hasTimeLeft()) {}
      AccountDetails("account details of slowId")
    case accountId => AccountDetails(s"account details of ${accountId}")
  }

  "AccountService" should {
    val accountService = _system.actorOf(Props(new AccountService(testBackend)))

    "work fine with valid account id" in {
      accountService ! GetAccountDetails("validId")
      expectMsg(AccountDetails("account details of validId"))
    }

    "open circuit after 2 failures" in {
      val accountService = _system.actorOf(Props(new AccountService(testBackend)))
      accountService ! GetAccountDetails("slowId")
      expectMsg(AccountDetails("account details of slowId"))

      EventFilter.warning("Circuit breaker is now open", occurrences = 1).intercept {
        accountService ! GetAccountDetails("slowId")
        expectMsg(AccountDetails("account details of slowId"))
      }

      accountService ! GetAccountDetails("slowId")
      expectMsg("Please try again later")
    }

    "half-open circuit after reset timeout" in {
      pending
    }

    "closed circuit after reset timeout" in {
      pending
    }

  }

}