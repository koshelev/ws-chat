package chat.netty

import java.net.InetSocketAddress

import io.netty.channel.ChannelFuture
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate

object NettyChat extends App {

  val certificate = new SelfSignedCertificate()
  val sslContext = SslContextBuilder.forServer( certificate.certificate(), certificate.privateKey() ).build()
  val close : Unit => ChannelFuture = ChatServer( new InetSocketAddress(8080),  sslContext )

  sys.addShutdownHook {
    close().syncUninterruptibly()
  }
}
