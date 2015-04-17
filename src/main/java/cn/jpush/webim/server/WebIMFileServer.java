package cn.jpush.webim.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import org.slf4j.LoggerFactory;

import cn.jpush.protocal.common.HttpInboundHandler;
import ch.qos.logback.classic.Logger;

public class WebIMFileServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(WebIMFileServer.class);
	
	public void start(int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                		ch.pipeline().addLast(new HttpRequestDecoder());
                                		ch.pipeline().addLast(new HttpResponseEncoder());
                                		ch.pipeline().addLast(new HttpContentCompressor());
                                		ch.pipeline().addLast(new HttpInboundHandler());
                                }
                            }).option(ChannelOption.SO_BACKLOG, 128) 
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
	
	public static void main(String[] args) throws InterruptedException {
			WebIMFileServer server = new WebIMFileServer();
			log.info("file Server listening on 9093...");
			try {
				server.start(9093);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

}
