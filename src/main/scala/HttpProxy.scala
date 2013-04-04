package se.nullable.smack

import akka.actor.{Actor, ActorRef, ActorContext, IO, IOManager, Props, Status}
import akka.util.{ByteString, Timeout}

import scala.util.matching.Regex
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import scala.concurrent.Future

import java.net.InetSocketAddress
import java.io.{PrintStream, ByteArrayOutputStream}


object HttpConstants {
	val CRLF = ByteString("\r\n")
	val COLON = ByteString(":")
	val SPACE = ByteString(" ")
}

object HttpIteratees {
	import HttpConstants._

	type Header = (ByteString, ByteString)
	case class Request(requestLine: ByteString, headers: Map[ByteString, ByteString], body: Option[ByteString])

	def readMultilineValue(initial: ByteString): IO.Iteratee[ByteString] = IO peek 1 flatMap {
		case SPACE => IO takeUntil CRLF flatMap (bytes => readMultilineValue(initial ++ bytes))
		case _ => IO Done initial
	}

	def readHeader = for {
		name <- IO takeUntil COLON
		_ <- IO takeWhile (_ == ' ')
		value <- IO takeUntil CRLF flatMap readMultilineValue
	} yield (name -> value)

	def readHeaders = {
		def step(found: List[Header]): IO.Iteratee[List[Header]] = IO peek 2 flatMap {
			case CRLF => IO takeUntil CRLF flatMap (_ => IO Done found)
			case _    => readHeader flatMap (header => step(header :: found))
		}
		step(Nil) map (_.toMap)
	}

	def readBody(headers: Map[ByteString, ByteString]) =
		if ((headers contains ByteString("Content-Length")) || (headers contains ByteString("Transfer-Encoding")))
			IO.takeAll map (Some(_))
		else
			IO Done None

	def readRequest = for {
		requestLine <- IO takeUntil CRLF
		headers <- readHeaders
		body <- readBody(headers)
	} yield Request(requestLine, headers, body)
}

// A handler for a single proxy connection, using Spray-IO for this, since akka.actor.IO currently (2.1.2) doesn't send IO.Closed for clients
case class HttpProxyRequest(address: InetSocketAddress, request: HttpIteratees.Request)
class HttpProxySubConnectionHandler(_ioBridge: ActorRef) extends spray.io.IOClient(_ioBridge) {
	import HttpConstants._

	import spray.util._
	import spray.io._

	var request: HttpIteratees.Request = null
	lazy val HttpIteratees.Request(requestLine, headers, body) = request

	var parent: ActorRef = null
	var buffer: ByteString = ByteString()

	def myReceive: Receive = {
		case HttpProxyRequest(address, request) =>
			parent = sender
			this.request = request
			self ! IOClient.Connect(address)

		case IOClient.Connected(socket) =>
			var usedHeaders = headers
			usedHeaders += ByteString("Connection") -> ByteString("close")  // We don't currently support keeping connections alive

			val serialized = HttpProxySubConnectionHandler.serializeRequest(HttpIteratees.Request(requestLine, usedHeaders, body))
			socket.ioBridge ! IOBridge.Send(socket, serialized.toByteBuffer)
		case Status.Failure(x) =>
			parent ! Status.Failure(x)
			context.stop(self)
		case IOClient.Received(socket, bytes) =>
			buffer ++= ByteString(bytes)
		case IOClient.Closed(socket, cause) =>
			parent ! buffer
			context.stop(self)
	}
	override def receive = myReceive orElse super.receive
}

object HttpProxySubConnectionHandler {
	import HttpConstants._
	import spray.io.IOExtension

	def apply()(implicit context: ActorContext) = new HttpProxySubConnectionHandler(IOExtension(context.system).ioBridge())

	def serializeHeaders(headers: Map[ByteString, ByteString]) = {
		headers.toSeq.foldLeft(ByteString()) { (seed, header) =>
			val (k, v) = header
			seed ++ k ++ COLON ++ SPACE ++ v ++ CRLF
		}
	}

	def serializeRequest(request: HttpIteratees.Request) = {
		val HttpIteratees.Request(requestLine, headers, body) = request
		var output = ByteString()
		output ++= requestLine ++ CRLF
		output ++= serializeHeaders(headers) ++ CRLF
		output ++= (body map (_ ++ CRLF) getOrElse ByteString())
		output
	}
}

class HttpProxy(host: InetSocketAddress, processManager: ActorRef, hostnameRegexes: Iterable[Regex]) extends Actor {
	import HttpIteratees.readRequest

	val state = IO.IterateeRef.Map.async[IO.Handle]()(context.dispatcher)

	override def preStart() {
		IOManager(context.system) listen host
	}

	def processRequest(socket: IO.SocketHandle): IO.Iteratee[Unit] = for {
		request <- readRequest
	} yield {
		import akka.pattern.ask
		implicit val dispatcher = context.system.dispatcher
		implicit val timeout = Timeout(10.seconds)

		val rawHost = request.headers.get(ByteString("Host")) map (_ decodeString "ASCII")
		val optHost = rawHost flatMap { host =>
			hostnameRegexes map (_ findFirstMatchIn host) collectFirst {
				case Some(x) => x.group(1)
			}
		}

		val futureAddress = optHost map ( host => (processManager ? QueryServiceAddress(host)).mapTo[Option[InetSocketAddress]]) getOrElse Future(None)

		futureAddress flatMap (_ match {
			case Some(address) => (context.actorOf(Props(HttpProxySubConnectionHandler())) ? HttpProxyRequest(address, request)).mapTo[ByteString]
			case None => Future(ByteString(s"""
HTTP 502 Bad Gateway
Content-Type: text/plain

Smack error: Unknown hostname "$rawHost" (parses to "$optHost")
"""))
		}) andThen {
			case Failure(error) =>
				val traceOS = new ByteArrayOutputStream
				val tracePS = new PrintStream(traceOS)
				error.printStackTrace(tracePS)
				tracePS.close()
				val trace = ByteString(traceOS.toByteArray)
				traceOS.close()

				socket.write(ByteString("""
HTTP 502 Bad Gateway
Content-Type: text/plain

Smack error: Internal exception occurred. Stack trace below:

""") ++ trace)
			case Success(v) => socket.write(v)
		} andThen {
			case _ => socket.close()
		}
	}

	def receive = {
		case IO.NewClient(server) =>
			val socket = server.accept()
			state(socket) flatMap (_ => processRequest(socket))

		case IO.Read(socket, bytes) =>
			state(socket)(IO Chunk bytes)

		case IO.Closed(socket, cause) =>
			state(socket)(IO.EOF)
			state -= socket
	}
}
