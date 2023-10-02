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
                // new LengthFieldBasedFrameDecoder(1024, 12, 4, 0, 0),
                new MessageCodec()
        );
        // encode
        LoginRequestMessage message = new LoginRequestMessage("zhangsan", "123", "张三");
        channel.writeOutbound(message);

        // // decode
        // ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        // new MessageCodec().encode(null, message, buf);
        //
        // ByteBuf s1 = buf.slice(0, 100);
        // ByteBuf s2 = buf.slice(100, buf.readableBytes() - 100);
        // s1.retain(); // 引用计数 2
        // channel.writeInbound(s1); // release 1
        // channel.writeInbound(s2);
    }
}
