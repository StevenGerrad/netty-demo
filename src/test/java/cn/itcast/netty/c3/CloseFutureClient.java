package cn.itcast.netty.c3;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Scanner;

@Slf4j
public class CloseFutureClient {
    /*
     * @param args:
     * @return void
     * @author wangjunyou
     * @description ：P67 客户端接受用户输入，并将信息不断发送给服务器端，不想发送信息后，输入一个 q 就取消
     * @date 2023/9/30 21:27
     */
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override // 在连接建立后被调用
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect(new InetSocketAddress("localhost", 8080));
        System.out.println(channelFuture.getClass());
        Channel channel = channelFuture.sync().channel();
        log.debug("{}", channel);
        new Thread(()->{
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();
                if ("q".equals(line)) {
                    channel.close(); // close 异步操作 1s 之后
                    // log.debug("处理关闭之后的操作"); // 不能在这里善后，因为 close 也是异步操作
                    break;
                }
                channel.writeAndFlush(line);
            }
        }, "input").start();

        // 获取 CloseFuture 对象， 1) 同步处理关闭， 2) 异步处理关闭
        // 方案一：同步处理关闭
        ChannelFuture closeFuture = channel.closeFuture();
        log.debug("waiting close...");
        closeFuture.sync();
        log.debug("处理关闭之后的操作");

        // 方案二：异步处理关闭
        // System.out.println(closeFuture.getClass());
        closeFuture.addListener((ChannelFutureListener) future -> {
            log.debug("处理关闭之后的操作");
            // 不加这个的话，客户端没有结束。会等待任务处理完成后才停止
            group.shutdownGracefully();
        });
    }
}

