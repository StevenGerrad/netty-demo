package cn.itcast.netty.c3;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Slf4j
public class TestNettyFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        EventLoop eventLoop = group.next();
        Future<Integer> future = eventLoop.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("执行计算");
                Thread.sleep(1000);
                return 70;
            }
        });
        // 同步方式
        // log.debug("等待结果");
        // log.debug("结果是 {}", future.get());

        // 异步方式
        future.addListener(new GenericFutureListener<Future<? super Integer>>(){
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                log.debug("接收结果:{}", future.getNow());
            }
        });

        // Jdk 和 netty的future对象，不是我们创建的，而是向线程池或eventLoop中提交任务后返回的future。
        // 即future的创建权和对结果的设置权不是我们控制的 —— 要采用Promise对象
    }
}
