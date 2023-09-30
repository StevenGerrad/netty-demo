package cn.itcast.netty.c3;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

/**
 * @projectName: netty-demo
 * @package: cn.itcast.netty.c3
 * @className: EventLoopServer
 * @author: Gerrad
 * @description:
 * @date: 2023/9/30 15:26
 * @version: 1.0
 */

@Slf4j
public class EventLoopServer {
    public static void main(String[] args) {
        // 细分2：创建一个独立的 EventLoopGroup（专门执行耗时较长的操作）
        EventLoopGroup group = new DefaultEventLoopGroup();
        new ServerBootstrap()
                // boss 和 worker
                // 细分1：boss 只负责 ServerSocketChannel 上 accept 事件
                //          （不用设置线程数1，NioServerSocketChannel 只会跟里面的一个 EventLoop 绑定，也没有更多的ServerSocketChannel了）
                //       worker 只负责 socketChannel 上的读写，线程数根据实际情况设置
                .group(new NioEventLoopGroup(), new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast("handler1", new ChannelInboundHandlerAdapter() {
                            @Override                                         // ByteBuf
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.debug(buf.toString(Charset.defaultCharset()));
                                // 一个worker处理很多channel，如果一个channel过于耗时，其它channel都会被拖慢了
                                ctx.fireChannelRead(msg); // 让消息传递给下一个handler
                            }
                        })
                        .addLast(group, "handler2", new ChannelInboundHandlerAdapter() {
                            @Override                                         // ByteBuf
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.debug(buf.toString(Charset.defaultCharset()));
                            }
                        });
                    }
                })
                .bind(8080);
    }
}

