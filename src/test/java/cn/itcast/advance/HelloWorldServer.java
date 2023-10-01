package cn.itcast.advance;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HelloWorldServer {
    static final Logger log = LoggerFactory.getLogger(HelloWorldServer.class);
    // void start() {
    //     NioEventLoopGroup boss = new NioEventLoopGroup();
    //     NioEventLoopGroup worker = new NioEventLoopGroup();
    //     try {
    //         ServerBootstrap serverBootstrap = new ServerBootstrap();
    //         serverBootstrap.channel(NioServerSocketChannel.class);
    //         serverBootstrap.option(ChannelOption.SO_RCVBUF, 10);    // 模拟半包线程，接受缓冲区
    //         serverBootstrap.group(boss, worker);
    //         serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
    //             @Override
    //             protected void initChannel(SocketChannel ch) throws Exception {
    //                 ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
    //             }
    //         });
    //         ChannelFuture channelFuture = serverBootstrap.bind(8080).sync();
    //         channelFuture.channel().closeFuture().sync();
    //     } catch (InterruptedException e) {
    //         log.error("server error", e);
    //     } finally {
    //         boss.shutdownGracefully();
    //         worker.shutdownGracefully();
    //         log.debug("stoped");
    //     }
    // }

    void start() {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);

            // 现在一般不用设置窗口大小，通信过程中协议会自动调整
            // 调整系统的接受缓冲区（滑动窗口）
            // serverBootstrap.option(ChannelOption.SO_RCVBUF, 10);    // 模拟半包线程，接受缓冲区
            // 调整netty的接受缓冲区（bytebuf）
            serverBootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(16, 16, 16));

            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                }
            });
            ChannelFuture channelFuture = serverBootstrap.bind(8080).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            log.debug("stoped");
        }
    }

    public static void main(String[] args) {
        new HelloWorldServer().start();
    }
}