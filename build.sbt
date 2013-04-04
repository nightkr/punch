scalaVersion := "2.10.1"

scalacOptions ++= Seq("-deprecation", "-feature")

seq(lsSettings :_*)

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2"

libraryDependencies += "io.spray" % "spray-io" % "1.1-M7"
