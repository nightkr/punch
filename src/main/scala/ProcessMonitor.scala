package se.nullable.smack

import akka.actor.{Actor, Props, Kill, OneForOneStrategy, SupervisorStrategy}
import SupervisorStrategy.Restart

import scala.sys.process.{Process, ProcessBuilder}

import java.io.File


case object RestartProcess
class ProcessMonitor(processBuilder: ProcessBuilder) extends Actor {
	var process: Process = null  // Option[T] not applicable, since this being null once the actor has actually started should be treated as an error

	object RestartProcessException extends Exception

	override def supervisorStrategy = OneForOneStrategy() {
		case RestartProcessException => Restart
	}

	override def preStart() {
		process = processBuilder.run()

		// Launch a separate actor that kills the actor on process termination
		context.actorOf(Props(new Actor {
			def receive = {
				case process: Process =>
					process.exitValue()
					sender ! RestartProcess  // Restart on crash
			}
		}), name = "kill_monitor") ! process
	}

	// Kill the process on actor termination
	override def postStop() {
		process.destroy()
		process.exitValue()  // Wait for the subprocess to fully shut down
	}

	def receive = {
		case RestartProcess =>
			throw RestartProcessException
	}
}

object ProcessMonitor {
	def apply(defaultCmd: String, smackfile: File, dir: File, port: Int): ProcessMonitor = {
		val cmd =
			if (smackfile.exists)
				Seq("sh", smackfile.getAbsolutePath)
			else
				Seq("sh", "-c", defaultCmd)

		new ProcessMonitor(Process(cmd, cwd = dir, "PORT" -> port.toString, "DIR" -> dir.getAbsolutePath()))
	}
}