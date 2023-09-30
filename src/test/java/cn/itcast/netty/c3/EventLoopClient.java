package cn.itcast.netty.c3;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class EventLoopClient {
    // public static void main(String[] args) throws InterruptedException {
    //     // 1. 启动类
    //     Channel channel = new Bootstrap()
    //             // 2. 添加 EventLoop（客户端可以用，也可以不用netty，用之前的nio也可以）
    //             .group(new NioEventLoopGroup())
    //             // 3. 选择客户端 channel 实现
    //             .channel(NioSocketChannel.class)
    //             // 4. 添加处理器
    //             .handler(new ChannelInitializer<NioSocketChannel>() {
    //                 @Override // 在连接建立后被调用
    //                 protected void initChannel(NioSocketChannel ch) throws Exception {
    //                     ch.pipeline().addLast(new StringEncoder());
    //                 }
    //             })
    //             // 5. 连接到服务器
    //             .connect(new InetSocketAddress("localhost", 8080))
    //             .sync()
    //             .channel();
    //     // channel.writeAndFlush("hello");
    //     System.out.println(channel);
    //     System.out.println("");
    // }


    public static void main(String[] args) throws InterruptedException {
        // 2. 带有 Future，Promise 的类型都是和异步方法配套使用，用来处理结果
        ChannelFuture channelFuture = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override // 在连接建立后被调用
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                // 1. 连接到服务器
                // 异步非阻塞, main 发起了调用（但main不关心结果），真正执行 connect 是 nio 线程
                // 1s 秒后连接才建立好，但是 main 会继续向下执行，所以下面要使用 sync 方法
                .connect(new InetSocketAddress("localhost", 8080));

        // 那么如何获取异步的结果呢？

        // 2.1 方法一：使用 sync 方法同步处理结果
        // channelFuture.sync(); // 阻塞住当前线程，直到nio线程连接建立完毕
        // Channel channel = channelFuture.channel();   // 仍由主线程等待结果
        // log.debug("{}", channel);
        // channel.writeAndFlush("hello, world");

        // 2.2 方法二：使用 addListener(回调对象) 方法异步处理结果。等结果的也不是主线程了
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            // 在 nio 线程连接建立好之后，会调用 operationComplete
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel channel = future.channel();
                log.debug("{}", channel);
                channel.writeAndFlush("hello, world");
            }
        });
    }
}
