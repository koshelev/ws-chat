package chat.netty

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.group.{ChannelGroup, DefaultChannelGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.websocketx.{WebSocketServerProtocolHandler, TextWebSocketFrame}
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslContext
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.util.concurrent.{ImmediateEventExecutor, Future}

class HttpRequestHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  def sendNotFound(ctx: ChannelHandlerContext): Unit = {
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
    ctx.writeAndFlush( response )
  }

  override def channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {
    if( request.getUri.startsWith("/chat") ) {
      ctx.fireChannelRead( request.retain() )
    } else {
      sendNotFound( ctx )
    }
  }
}

class TextWebSocketFrameHandler(channels: ChannelGroup) extends SimpleChannelInboundHandler[TextWebSocketFrame] {

  override def userEventTriggered(ctx: ChannelHandlerContext, event: Any) : Unit = event match {
    case WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE =>
      ctx.pipeline().remove( classOf[HttpRequestHandler] )
      channels.add( ctx.channel() )

    case _ =>
      super.userEventTriggered(ctx, event)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame): Unit = {
    //channels.writeAndFlush( msg.retain() )
  }
}

private[this] class ChatServerInitializer(sslContext: SslContext, channels: ChannelGroup) extends ChannelInitializer[Channel] {
  override def initChannel(ch: Channel): Unit = {
    ch.pipeline()
//      .addLast(sslContext.newHandler(ch.alloc()))
      .addLast( new HttpServerCodec() )
      .addLast( new HttpObjectAggregator(64 * 1024) )
      .addLast( new HttpRequestHandler )
      .addLast( new WebSocketServerProtocolHandler("/chat"))
      .addLast( new TextWebSocketFrameHandler(channels) )
  }
}

object ChatServer {

  def apply( address: InetSocketAddress, sslContext: SslContext ) : Unit => ChannelFuture = {
    val channels = new DefaultChannelGroup( ImmediateEventExecutor.INSTANCE )
    val eventLoopGroup = new NioEventLoopGroup()
    val bootstrap = new ServerBootstrap()
      .group( eventLoopGroup )
      .channel( classOf[NioServerSocketChannel] ).childHandler( new ChatServerInitializer(sslContext, channels) )

    val binding = bootstrap.bind( address )
    binding.syncUninterruptibly()

    (Unit) => {
      val closeFuture = binding.channel().close()
      channels.close()
      eventLoopGroup.shutdownGracefully()
      closeFuture
    }
  }

}
