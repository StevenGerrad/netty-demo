package cn.itcast.netty.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.itcast.nio.c2.ByteBufferUtil.debugAll;

@Slf4j
public class MultiThreadServer0 {
    public static void main(String[] args) throws IOException {
        // Boss只负责建立连接，Worker复制执行事件
        Thread.currentThread().setName("boss");
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector boss = Selector.open();
        SelectionKey bossKey = ssc.register(boss, 0, null);
        bossKey.interestOps(SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(8080));

        Worker worker = new Worker("worker-0");
        worker.register();

        while(true) {
            boss.select();
            Iterator<SelectionKey> iter = boss.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    // 建立了新的连接后，读写交给worker
                    log.debug("connected...{}", sc.getRemoteAddress());
                    // 2. 关联 selector
                    log.debug("before register...{}", sc.getRemoteAddress());

                    sc.register(worker.selector, SelectionKey.OP_READ, null);
                    log.debug("after register...{}", sc.getRemoteAddress());
                }
            }
        }
    }
    static class Worker implements Runnable{
        private Thread thread;
        private Selector selector;
        private String name;
        private volatile boolean start = false; // 还未初始化

        public Worker(String name) {
            this.name = name;
        }

        // 初始化线程，和 selector
        public void register() throws IOException {
            // 线程只创建一次
            if(!start) {
                // Thread要给一个任务，把Worker本身当任务就行
                thread = new Thread(this, name);
                selector = Selector.open();
                thread.start();

                start = true;
            }
        }

        /**
         * @description 注意 Worker的职责，即监控读写事件
         * @author wangjunyou
         * @date 2023/9/29 17:06
         */
        @Override
        public void run() {
            while(true) {
                try {
                    selector.select(); // worker-0  阻塞
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocate(16);
                            SocketChannel channel = (SocketChannel) key.channel();
                            log.debug("read...{}", channel.getRemoteAddress());
                            channel.read(buffer);
                            buffer.flip();
                            debugAll(buffer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}