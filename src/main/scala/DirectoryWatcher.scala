package se.nullable.smack

import akka.actor.Actor

import scala.collection.JavaConversions._

import java.io.File
import java.nio.file.{Path, WatchService, StandardWatchEventKinds}


// On the applicable events, DirectoryWatcher sends the File* messages to it's parent actor. file is absolute. Same events also thrown for directories.
case class FileCreated(file: File)
case class FileDeleted(file: File)
case class FileModified(file: File)
class DirectoryWatcher(dir: File) extends Actor {
	// This is based on the java.nio.file APIs that only work with java.nio.file.Path paths, but take a java.io.File instead for consistency
	val path = dir.toPath
	var watchService: WatchService = null

	override def preStart() {
		watchService = path.getFileSystem().newWatchService()
		path.register(watchService,
			StandardWatchEventKinds.ENTRY_CREATE,
			StandardWatchEventKinds.ENTRY_DELETE,
			StandardWatchEventKinds.ENTRY_MODIFY)
		self ! Poll
	}

	override def postStop() {
		watchService.close()
	}

	case object Poll
	def receive = {
		case Poll =>
			val key = watchService.take()
			for (event <- key.pollEvents) {
				context.parent ! (event.kind match {
					case StandardWatchEventKinds.ENTRY_CREATE => FileCreated
					case StandardWatchEventKinds.ENTRY_DELETE => FileDeleted
					case StandardWatchEventKinds.ENTRY_MODIFY => FileModified
				})(new File(dir, event.context.toString))
			}
			key.reset()
			self ! Poll
	}
}