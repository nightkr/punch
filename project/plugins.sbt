addSbtPlugin("com.typesafe.akka" % "akka-sbt-plugin" % "2.1.2")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")
