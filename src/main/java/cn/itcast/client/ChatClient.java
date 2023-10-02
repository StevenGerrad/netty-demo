package cn.itcast.client;

import cn.itcast.message.LoginRequestMessage;
import cn.itcast.message.LoginResponseMessage;
import cn.itcast.protocol.MessageCodecSharable;
import cn.itcast.protocol.ProcotolFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ChatClient {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        // LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();

        // 帮助要执行 channelRead 的nio线程 和 system in 线程进行通信
        CountDownLatch WAIT_FOR_LOGIN = new CountDownLatch(1);
        AtomicBoolean LOGIN = new AtomicBoolean(false);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ProcotolFrameDecoder());
                    // ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(MESSAGE_CODEC);
                    ch.pipeline().addLast("client handler", new ChannelInboundHandlerAdapter(){

                        // 接受服务器的响应消息
                        // channelRead 方法是在nio中被调用的，而下面等待后续操作的是 system in 线程，两个线程需要通信 - CountDownLatch
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
                            log.debug("msg: {}", msg);
                            if(msg instanceof LoginResponseMessage){
                                LoginResponseMessage response = (LoginResponseMessage) msg;
                                if(response.isSuccess()){
                                    // 如果登录成功
                                    LOGIN.set(true);
                                }
                                // 唤醒system in 线程
                                WAIT_FOR_LOGIN.countDown();
                            }

                        }
                        // 在连接建立后触发 active 事件
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception{
                            // super.channelActive(ctx);
                            // 首先登录，输入用户名、密码，构建LoginRequestMessage，启动一个新的线程，与netty独立
                            // 负责接受控制台输入，负责向服务器发送各种消息。不创建线程的话用nio线程不好，而且用户输入是阻塞的，不能让它阻塞住其它io线程
                            new Thread(() -> {
                                Scanner scanner = new Scanner(System.in);
                                System.out.println("请输入用户名：");
                                String username = scanner.nextLine();
                                System.out.println("请输入密码：");
                                String password = scanner.nextLine();

                                // 构造消息对象
                                LoginRequestMessage message = new LoginRequestMessage(username, password);
                                // 发送消息
                                ctx.writeAndFlush(message);

                                System.out.println("等待后续操作...");
                                // try{
                                //     System.in.read();
                                // } catch (IOException e){
                                //     e.printStackTrace();
                                // }
                                try {
                                    WAIT_FOR_LOGIN.await();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                // 如果登录失败
                                if(!LOGIN.get()){
                                    ctx.channel().close();
                                    return;
                                }
                                while(true){
                                    System.out.println("==================================");
                                    System.out.println("send [username] [content]");
                                    System.out.println("gsend [group name] [content]");
                                    System.out.println("gcreate [group name] [m1,m2,m3...]");
                                    System.out.println("gmembers [group name]");
                                    System.out.println("gjoin [group name]");
                                    System.out.println("gquit [group name]");
                                    System.out.println("quit");
                                    System.out.println("==================================");
                                    // String command = null;
                                    // try {
                                    //     command = scanner.nextLine();
                                    // } catch (Exception e) {
                                    //     break;
                                    // }
                                    // if(EXIT.get()){
                                    //     return;
                                    // }
                                    // String[] s = command.split(" ");
                                    // switch (s[0]){
                                    //     case "send":
                                    //         ctx.writeAndFlush(new ChatRequestMessage(username, s[1], s[2]));
                                    //         break;
                                    //     case "gsend":
                                    //         ctx.writeAndFlush(new GroupChatRequestMessage(username, s[1], s[2]));
                                    //         break;
                                    //     case "gcreate":
                                    //         Set<String> set = new HashSet<>(Arrays.asList(s[2].split(",")));
                                    //         set.add(username); // 加入自己
                                    //         ctx.writeAndFlush(new GroupCreateRequestMessage(s[1], set));
                                    //         break;
                                    //     case "gmembers":
                                    //         ctx.writeAndFlush(new GroupMembersRequestMessage(s[1]));
                                    //         break;
                                    //     case "gjoin":
                                    //         ctx.writeAndFlush(new GroupJoinRequestMessage(username, s[1]));
                                    //         break;
                                    //     case "gquit":
                                    //         ctx.writeAndFlush(new GroupQuitRequestMessage(username, s[1]));
                                    //         break;
                                    //     case "quit":
                                    //         ctx.channel().close();
                                    //         return;
                                    // }
                                }
                            }, "system in").start();
                        }
                    });
                }
            });
            Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
            // 这里channel已经建立好了，可以发送登陆消息
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
