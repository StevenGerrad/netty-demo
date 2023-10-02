package cn.itcast.protocol;

import cn.itcast.message.LoginRequestMessage;
import cn.itcast.protocol.MessageCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;

public class TestMessageCodec {
    /**
     * @description 如果报错 MessageCodec is not allowed to be shared 要把 MessageCodec 类 上的 @ChannelHandler.Sharable 去掉
     */
    public static void main(String[] args) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(
                new LoggingHandler(),
                // 配置帧解码器解决黏包半包问题
                new LengthFieldBasedFrameDecoder(1024, 12, 4, 0, 0),
                new MessageCodec()
        );
        // encode
        LoginRequestMessage message = new LoginRequestMessage("zhangsan", "123", "张三");
        channel.writeOutbound(message);

        // decode
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        new MessageCodec().encode(null, message, buf);
        // channel.writeInbound(buf);

        // 用切片模拟黏包半包
        ByteBuf s1 = buf.slice(0, 100);
        ByteBuf s2 = buf.slice(100, buf.readableBytes() - 100);
        s1.retain(); // 引用计数 2
        channel.writeInbound(s1); // 会调用 release 1，引用计数 -1
        // 不适用帧解码器，只发s1会报错 java.lang.IndexOutOfBoundsException: readerIndex(16) + length(223) exceeds writerIndex(100):
        // 用了帧解码器之后发现数据不完整就不会发送给下一个handler
        channel.writeInbound(s2);
    }
}
