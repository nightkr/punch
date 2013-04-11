scalaVersion := "2.10.1"

scalacOptions ++= Seq("-deprecation", "-feature")

seq(lsSettings: _*)

seq(distSettings: _*)

outputDirectory in Dist := file("target/punch-dist")

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-kernel" % "2.1.2"

libraryDependencies += "io.spray" % "spray-io" % "1.1-M7"
