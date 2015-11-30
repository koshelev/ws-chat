package akkachat

import akka.actor._
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.stream.{OverflowStrategy, Materializer}
import akka.stream.scaladsl.{Source, Sink, Flow}

import scala.collection.mutable

object ChatActor {

  sealed trait ChatMessage
  case class UserJoined(name: String, ref: ActorRef) extends ChatMessage
  case class UserLeft(name: String) extends ChatMessage
  case class IncomingMessage( from: String, text: String) extends ChatMessage

  case class OutMessage(text: String)
}

class ChatActor extends Actor {
  import ChatActor._
  import scala.concurrent.duration._

  var sessions : mutable.Set[ActorRef] = mutable.Set.empty

  implicit val ec = context.dispatcher
//  val pingCancellable = context.system.scheduler.schedule( 10.seconds, 10.seconds, self, IncomingMessage("admin", "ping"))

  override def receive: Receive = {
    case UserJoined(name, ref) =>
      broadcast(s"$name joined")
      sessions += ref
      context.watch( ref )

    case UserLeft(name) =>
      broadcast(s"$name left")

    case IncomingMessage(from, text) =>
      broadcast(s"$from: $text")

    case Terminated(ref) =>
      sessions -= ref
  }

  def broadcast(message: String) : Unit = {
    val msg = OutMessage(message)
    sessions.foreach( _ ! msg )
  }
}

class WebSocketChat(implicit system: ActorSystem, mat: Materializer) {
  import akka.http.scaladsl.server.Directives._
  import akkachat.ChatActor._

  val chat = system.actorOf( Props( new ChatActor() ), "chat" )

  def route = get {
    path("chat" / Segment) { name =>
      handleWebsocketMessages( chatFlow(name) )
    }
  }

  def chatFlow(userName: String) : Flow[Message, Message, Unit] = {
    val inSink = Flow[Message].collect {
      case TextMessage.Strict(msg) =>
        IncomingMessage(userName, msg)
    }.to( Sink.actorRef[ChatMessage]( chat, UserLeft(userName) ) )

    val outSource = Source.actorRef[OutMessage](1, OverflowStrategy.fail)
      .map[Message](x => TextMessage(x.text) )
      //.mapMaterializedValue(chat ! UserJoined(userName, _))


    Flow.fromSinkAndSource(Sink.ignore, outSource)
  }
}
