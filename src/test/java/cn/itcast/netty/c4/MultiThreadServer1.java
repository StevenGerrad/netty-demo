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
public class MultiThreadServer1 {
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
        // worker.register();

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

                    // 把该语句从 new Worker 后拿到这里，有一定几率被先执行
                    // worker.register();  // 初始化 selector 启动worker0
                    // 如果下面worker的run()中的selector.select() 先执行了，就会在哪阻塞住，不会执行这个
                    // sc.register(worker.selector, SelectionKey.OP_READ, null);   // 在boss中执行

                    // 把 worker.register(); 拿下来有一定几率解决 Worker.run()中的selector.select() 和
                    // sc.register(worker.selector... 阻塞的问题，但是再开一个Client的话还是会阻塞住
                    // 根本原因是都使用了同一个 selector，并且一个的执行在worker-0线程，一个的执行在boss线程

                    // 第一个解决方法：把 selector.select() 和 sc.register(worker.selector... 放到一个线程里
                    worker.register(sc);    // boss线程调用，初始化selector，启动worker-0

                    log.debug("after register...{}", sc.getRemoteAddress());
                }
            }
        }
    }



    /**
     * P44
     * boss 接受连接（accept）
     * boss 调用 worker.register(sc); 进行初始化
     *      如果是第一次就会创建selector、线程，并启动线程
     *      向队列中加入任务
     * worker-0 已经启动了，执行run，在 selector.select(); 这里阻塞住
     * boss 唤醒 （执行selector.wakeup();） TODO：不会提前唤醒吗？
     * worker-0 继续执行，从队列中拿到任务事件。但其实不一定有任务，然后循环回来继续在 selector.select(); 阻塞
     */

    static class Worker implements Runnable{
        private Thread thread;
        private Selector selector;
        private String name;

        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();  // 在两个线程之间传递数据

        private volatile boolean start = false; // 还未初始化

        public Worker(String name) {
            this.name = name;
        }

        // 初始化线程，和 selector
        public void register(SocketChannel sc) throws IOException {
            // 线程只创建一次
            if(!start) {
                selector = Selector.open();
                // Thread要给一个任务，把Worker本身当任务就行
                thread = new Thread(this, name);
                thread.start();
                start = true;
            }
            // sc.register(selector, SelectionKey.OP_READ, null);  // 虽然这行代码在worker里，但其实还是boss线程执行

            // 向队列添加了任务，单这个任务并没有立刻执行
            queue.add(()->{
                try{
                    sc.register(selector, SelectionKey.OP_READ, null);
                } catch (ClosedChannelException e){
                    e.printStackTrace();
                }
            });
            selector.wakeup();  // 唤醒 selector
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
                    selector.select(); // 在 worker-0 执行阻塞 / 或调用 wakeup 方法使得事件结束

                    Runnable task = queue.poll();
                    if(task != null) {
                        task.run(); // 执行了 sc.register(selector, SelectionKey.OP_READ, null);
                    }

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
