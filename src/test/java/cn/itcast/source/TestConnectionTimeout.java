package cn.itcast.source;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestConnectionTimeout {
    public static void main(String[] args) {
        // 1. 客户端通过 .option() 方法配置参数 给 SocketChannel 配置参数

        // 2. 服务器端
        // new ServerBootstrap().option() // 是给 ServerSocketChannel 配置参数
        // new ServerBootstrap().childOption() // 给 SocketChannel 配置参数

        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .channel(NioSocketChannel.class)
                    .handler(new LoggingHandler());
            // 超时时间设为 5000 ： Caused by: java.net.ConnectException: Connection refused: no further information 不会等网络异常，会先报连接异常
            // 超时时间设为 300：io.netty.channel.ConnectTimeoutException: connection timed out: /127.0.0.1:8080 超时异常
            ChannelFuture future = bootstrap.connect("127.0.0.1", 8080);
            future.sync().channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("timeout");
        } finally {
            group.shutdownGracefully();
        }
    }
}
