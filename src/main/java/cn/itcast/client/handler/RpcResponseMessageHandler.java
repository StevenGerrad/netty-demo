package cn.itcast.client.handler;

import cn.itcast.message.RpcRequestMessage;
import cn.itcast.message.RpcResponseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class RpcResponseMessageHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {

    // 一个service的多次方法调用都需要不同的Promise对象，不同Promise就靠sequenceId对应
    // Map：序号 用来接收结果的 promise 对象
    public static final Map<Integer, Promise<Object>> PROMISES = new ConcurrentHashMap<>();

    /**
     * @description P136 RpcClientManager 中的代理对象在方法调用时都是在主线程中被执行，但是服务器真正返回响应消息，
     *      是 RpcResponseMessageHandler 进行处理，所在的是 netty的nio线程，即涉及到两个线程通信，要使用Promise。
     *      Promise 就是一个容器，可以在多个线程之间交换结果，而且Promise也需要多个
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponseMessage msg) throws Exception {
        log.debug("{}", msg);
        // 拿到空的 promise（cn.itcast.client.RpcClientManager.getProxyService 放进去的）
        Promise<Object> promise = PROMISES.remove(msg.getSequenceId());
        if (promise != null) {
            Object returnValue = msg.getReturnValue();
            Exception exceptionValue = msg.getExceptionValue();
            if(exceptionValue != null) {
                promise.setFailure(exceptionValue);
            } else {
                promise.setSuccess(returnValue);
            }
        }
    }
}
