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
    // public static void main(String[] args) throws Exception {
    //     EmbeddedChannel channel = new EmbeddedChannel(
    //             new LoggingHandler(),
    //             // 配置帧解码器解决黏包半包问题
    //             new LengthFieldBasedFrameDecoder(1024, 12, 4, 0, 0),
    //             new MessageCodec()
    //     );
    //     // encode
    //     LoginRequestMessage message = new LoginRequestMessage("zhangsan", "123", "张三");
    //     channel.writeOutbound(message);
    //
    //     // decode
    //     ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
    //     new MessageCodec().encode(null, message, buf);
    //     // channel.writeInbound(buf);
    //
    //     // 用切片模拟黏包半包
    //     ByteBuf s1 = buf.slice(0, 100);
    //     ByteBuf s2 = buf.slice(100, buf.readableBytes() - 100);
    //     s1.retain(); // 引用计数 2
    //     channel.writeInbound(s1); // 会调用 release 1，引用计数 -1
    //     // 不适用帧解码器，只发s1会报错 java.lang.IndexOutOfBoundsException: readerIndex(16) + length(223) exceeds writerIndex(100):
    //     // 用了帧解码器之后发现数据不完整就不会发送给下一个handler
    //     channel.writeInbound(s2);
    // }

    /**
     * P106 可以添加一个handler实例，被多个channel所共享吗？要具体分析
     */
    public static void main(String[] args) throws Exception {
        // 共享 handler有风险，一个实例可能被多个eventLoop用到（eventLoop才是真正的工人）
        // 工人1 收到半包 1234，记录下来
        // 工人2 收到半包 1234
        // 可能就把工人1的一部分数据和工人2的一部分数据拼成一个完整消息了，出现错误。
        // 所以 LengthFieldBasedFrameDecoder 由于记录了消息中间状态，所以不安全
        LengthFieldBasedFrameDecoder FRAME_DECODER= new LengthFieldBasedFrameDecoder(1024, 12, 4, 0, 0);
        // LoggingHandler 不会保存状态信息，是线程安全的。netty中有 @Sharable 注解，标记了它是安全的。
        LoggingHandler LOGGING_HANDLER = new LoggingHandler();

        EmbeddedChannel channel = new EmbeddedChannel(
                // 配置帧解码器解决黏包半包问题
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
