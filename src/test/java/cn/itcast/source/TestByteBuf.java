package cn.itcast.source;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestByteBuf {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new LoggingHandler());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                // ByteBuf buf = ctx.alloc().buffer(); // 获取分配器对象
                                // log.debug("alloc buf {}", buf);

                                log.debug("receive buf {}", msg);
                                System.out.println("");
                            }
                        });
                    }
                }).bind(8080);
        // 查看源码后可知 ：
        // -Dio.netty.allocator.type=unpooled 控制是否使用池化
        // -Dio.netty.noPreferDirect=true 控制是否不使用直接内存

        // P129 发现还是直接内存 receive buf UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeNoCleanerDirectByteBuf(ridx: 0, widx: 6, cap: 1024)
        // 因为在io操作，网络读写数据的时候，直接内存效率更高。因而netty对io的读写操作强制使用了直接内存
    }
}
