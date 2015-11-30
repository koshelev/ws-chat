package akkachat

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.util.{Failure, Success}

object AkkaChat extends App {

  implicit val system = ActorSystem("chat")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val conf = system.settings.config
  val service = new WebSocketChat

  val host = conf.getString("chat.host")
  val port = conf.getInt("chat.port")

  val bindingFuture = Http().bindAndHandle(service.route, host, port)

  bindingFuture.onComplete {
    case Success(binding) =>
      system.log.info( s"Service online on $binding" )

    case Failure(t) =>
      system.log.error(t, "Service failed to start")
      sys.exit( -1 )
  }

  sys.addShutdownHook {
    system.terminate()
  }

}
