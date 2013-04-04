package se.nullable.smack

import akka.actor.{Extension, ExtensionId, ExtensionIdProvider, ExtendedActorSystem}

import com.typesafe.config.Config

import scala.concurrent.duration.Duration
import scala.sys.process._
import scala.collection.JavaConversions._

import java.util.concurrent.TimeUnit
import java.io.File
import java.net.InetSocketAddress

 
class SettingsImpl(config: Config) extends Extension {
	object Port {
		val Start = config.getInt("smack.port.start")
		val Increment = config.getInt("smack.port.increment")
	}

	object Proxy {
		object Http {
			val Host = config.getString("smack.proxy.http.host")
			val Port = config.getInt("smack.proxy.http.port")
			val Address = new InetSocketAddress(Host, Port)

			val Hostnames = config.getStringList("smack.proxy.http.hostnameRegexes") map (_.r)
		}
	}

	val DefaultCommand = config.getString("smack.defaultCommand")
	val Smackfile = config.getString("smack.smackfile")
	
	private val RawDirectory = config.getString("smack.directory")
	val Directory = new File(Seq("sh", "-c", s"echo $RawDirectory").!!.trim)  // Hacky and evil, but the best way I've found so far for expanding the path
}


object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {
	override def lookup = Settings
	override def createExtension(system: ExtendedActorSystem) = new SettingsImpl(system.settings.config)
}
