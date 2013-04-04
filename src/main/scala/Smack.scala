package se.nullable.smack

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import java.net.InetSocketAddress
import java.io.File


case class ServiceData(port: Int)
case class QueryServiceAddress(name: String)
class ProcessManager extends Actor {
	val settings = Settings(context.system)

	var port = settings.Port.Start
	var services = Map[String, ServiceData]()
	var runningServices = Map[String, ActorRef]()

	override def preStart() {
		context.actorOf(Props(new DirectoryWatcher(settings.Directory)), name = "watcher")
		for (file <- settings.Directory.listFiles) {
			self ! FileCreated(file)
		}

		context.actorOf(Props(new HttpProxy(settings.Proxy.Http.Address, self, settings.Proxy.Http.Hostnames)), name = "proxy")
	}

	def receive = {
		case FileCreated(dir) => receive(FileModified(dir))  // Treat these as equivalent for now; they should both trigger the same action anyway
		case FileModified(dir) =>
			if (dir.isDirectory()) {
				val serviceName = dir.getName

				if (!(services contains serviceName)) {
					services += serviceName -> ServiceData(port)
					port += settings.Port.Increment
				}
				val data = services(serviceName)

				if (runningServices.get(serviceName) map (!_.isTerminated) getOrElse false) {
					//context.system.stop(runningServices(serviceName))
					runningServices(serviceName) ! RestartProcess
				} else {
					runningServices += serviceName -> context.actorOf(Props(ProcessMonitor(settings.DefaultCommand, new File(dir, settings.Smackfile), dir, data.port)), name = s"process_$serviceName")
				}
			}
		case FileDeleted(dir) =>
			val serviceName = dir.getName
			if (runningServices contains serviceName) {
				context.system.stop(runningServices(serviceName))
				runningServices -= serviceName
			}
		case QueryServiceAddress(name) =>
			sender ! (services.get(name) map (s => new InetSocketAddress("localhost", s.port)))
	}
}


object Smack extends App {
	println("Press enter to stop")
	val system = ActorSystem("Smack")
	system.actorOf(Props[ProcessManager], name = "smack")
	//val actor = system.actorOf(Props(ForemanMonitor(new java.io.File("."), 11000)))
	readLine()
	system.shutdown()
}