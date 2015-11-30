import java.net.InetAddress

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class ChatSimulation extends Simulation {

  val feeder = Iterator.from(1).map( x => Map( "userName" -> s"User$x"))

  val addresses = Array("127.0.0.1")

  val usersPerAddress = 5000

  val injectionTime = 10.seconds

  val connectScenario = scenario("")
    .feed(feeder)
    .exec(ws("Connect to WS").open("ws://localhost:8080/chat/${userName}"))
    .during(90.seconds){
      exec(ws("Send Text").sendText("pingpongpingpongpingpongpingpongpingpongpingpongpingpongpingpongpingpongpingpong")).pause(1.second)
    }
//   .pause(injectionTime + 20.seconds)
    .exec(ws("Close WS").close)

  val steps = addresses.map { x =>
    connectScenario.copy( name = s"ws on $x")
      .inject( rampUsers(usersPerAddress) over injectionTime )
      .protocols( http.localAddress( InetAddress.getByName(x) ) )
  }

  setUp(
    steps: _*
  )

}