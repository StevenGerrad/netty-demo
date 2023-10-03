package cn.itcast.client;

import cn.itcast.client.handler.RpcResponseMessageHandler;
import cn.itcast.message.RpcRequestMessage;
import cn.itcast.protocol.MessageCodecSharable;
import cn.itcast.protocol.ProcotolFrameDecoder;
import cn.itcast.protocol.SequenceIdGenerator;
import cn.itcast.server.service.HelloService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

/**
 * P134 重构RpcClient，主要职责就是创建channel（向服务器发送消息都是通过channel发送的）
 */
@Slf4j
public class RpcClientManager {

//     public static void main(String[] args) {
//         HelloService service = getProxyService(HelloService.class);
//         System.out.println(service.sayHello("zhangsan"));
// //        System.out.println(service.sayHello("lisi"));
// //        System.out.println(service.sayHello("wangwu"));
//     }

    /**
     * @description 创建代理类，内部会把方法调用转换成消息的发送
     * @param serviceClass 接口类型
     */
    public static <T> T getProxyService(Class<T> serviceClass) {
        // 用jdk自带的代理
        ClassLoader loader = serviceClass.getClassLoader();
        Class<?>[] interfaces = new Class[]{serviceClass};
        // eg. sayHello  "张三"
        Object o = Proxy.newProxyInstance(loader, interfaces, (proxy, method, args) -> {
            // 1. 将方法调用转换为 消息对象（RpcRequestMessage）
            int sequenceId = SequenceIdGenerator.nextId();
            RpcRequestMessage msg = new RpcRequestMessage(
                    sequenceId,
                    serviceClass.getName(),
                    method.getName(),
                    method.getReturnType(),
                    method.getParameterTypes(),
                    args
            );
            // 2. 将消息对象发送出去
            getChannel().writeAndFlush(msg);

            // 一个service的多次方法调用都需要不同的Promise对象，不同Promise就靠sequenceId对应
            // 3. 准备一个空 Promise 对象，来接收结果             指定 promise 对象异步接收结果线程
            DefaultPromise<Object> promise = new DefaultPromise<>(getChannel().eventLoop());
            RpcResponseMessageHandler.PROMISES.put(sequenceId, promise);


            // 4. 等待 promise 结果（同步，.await()等）
            promise.await();

            // 如果是异步等待 promise 结果
            // promise.addListener(future -> {
            //    // 这个线程从 getChannel().eventLoop() 中来
            //    // TODO：对于线程的问题最终一定要再整理一下
            // });

            if(promise.isSuccess()) {
                // 调用正常
                return promise.getNow();
            } else {
                // 调用失败
                throw new RuntimeException(promise.cause());
            }
        });
        return (T) o;
    }

    // TODO: 教程里没加 volatile
    private static volatile Channel channel = null;
    private static final Object LOCK = new Object();

    /**
     * @description 单例模式（双重检查锁） 获取唯一的 channel 对象
     * @return Channel
     * @author wangjunyou
     * @date 2023/10/3 21:06
     */
    public static Channel getChannel() {
        if (channel != null) {
            return channel;
        }
        synchronized (LOCK) { //  t2
            if (channel != null) { // t1
                return channel;
            }
            initChannel();
            return channel;
        }
    }

    /**
     * @description 初始化 channel 方法
     *      但不能来一个就创建一个channel，应该只执行一次。——单例模式
     * @author wangjunyou
     * @date 2023/10/3 21:05
     */
    private static void initChannel() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        RpcResponseMessageHandler RPC_HANDLER = new RpcResponseMessageHandler();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(group);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ProcotolFrameDecoder());
                ch.pipeline().addLast(LOGGING_HANDLER);
                ch.pipeline().addLast(MESSAGE_CODEC);
                ch.pipeline().addLast(RPC_HANDLER);
            }
        });
        try {
            channel = bootstrap.connect("localhost", 8080).sync().channel();
            // 在这里.sync()阻塞很不优雅，关闭的逻辑要用异步方式改造
            // channel.closeFuture().sync();
            channel.closeFuture().addListener(future -> {
                group.shutdownGracefully();
            });
        } catch (Exception e) {
            log.error("client error", e);
        }
    }

    public static void main(String[] args) {
        // P133 把方法抽取了出来
        // P134 不能每次都创建channel，实现了单例模式
        // getChannel().writeAndFlush(new RpcRequestMessage(
        //         1,
        //         "cn.itcast.server.service.HelloService",
        //         "sayHello",
        //         String.class,
        //         new Class[]{String.class},
        //         new Object[]{"张三"}
        // ));

        // 但是一个Rpc调用者的使用习惯应该是这样的，类似直接使用Service服务层
        HelloService service = getProxyService(HelloService.class);    // 举例，下面应该创建代理类具体实现这种调用方式
        System.out.println(service.sayHello("zhangsan"));
        System.out.println(service.sayHello("李四"));


    }
}
