import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform._

object Build extends Build {

  lazy val akkaPatterns = Project(
    "akka-patterns",
    file("."),
    settings = commonSettings ++ Seq(
      organization := "com.typesafe.training",
      version := "2.0.0",
      scalaVersion := Version.scala,
      scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-feature",
        "-Xlint",
        "-language:_",
        "-target:jvm-1.6",
        "-encoding", "UTF-8"
      ),
      libraryDependencies ++= Seq(
        Dependency.Compile.akkaActor,
        Dependency.Compile.akkaAgent,
        Dependency.Compile.akkaRemote,
        Dependency.Compile.akkaSlf4j,
        Dependency.Compile.logbackClassic,
        Dependency.Test.akkaTestkit,
        Dependency.Compile.akkaCluster,
        Dependency.Test.scalaTest
      ),
      retrieveManaged := true
    )
  )

  def commonSettings =
    Defaults.defaultSettings ++ 
    scalariformSettings

  object Version {
    val scala = "2.10.2"
    val akka = "2.1.4"
  }

  object Dependency {

    object Compile {
      val akkaActor = "com.typesafe.akka" %% "akka-actor" % Version.akka
      val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.akka
      val akkaAgent = "com.typesafe.akka" %% "akka-agent" % Version.akka
      val akkaRemote = "com.typesafe.akka" %% "akka-remote" % Version.akka
      val akkaCluster = "com.typesafe.akka" %% "akka-cluster-experimental" % Version.akka
      val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.0.9"
    }

    object Test {
      val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % Version.akka % "test"
      val scalaTest = "org.scalatest" %% "scalatest" % "2.0.M5b" % "test"
    }
  }
}
