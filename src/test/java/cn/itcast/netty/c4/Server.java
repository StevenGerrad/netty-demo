package cn.itcast.netty.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static cn.itcast.netty.c1.ByteBufferUtil.debugRead;


@Slf4j
public class Server {
    public static void main(String[] args) throws IOException {
        // 1. 创建 Selector，管理多个channel
        Selector selector = Selector.open();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        // 2. 建立selector和channel的联系（注册）
        // SelectionKey 就是将来事件发生后，通过它可以知道事件和那个channel的事件
        SelectionKey sscKey = ssc.register(selector, 0, null);
        // keu 只关注 accept 事件
        sscKey.interestOps(SelectionKey.OP_ACCEPT);
        log.debug("register key:{}", sscKey);

        ssc.bind(new InetSocketAddress(8080));
        while(true){
            // 3. select 方法，没有事件发生，线程阻塞，有事件，线程才会恢复运行
            // select 事件未处理时，它不会阻塞，事件发生后要么处理，要么取消，不能置之不理
            selector.select();
            // 4. 处理事件，selectedKeys 内部包含了所有发生的事件
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while(iter.hasNext()){
                SelectionKey key = iter.next();
                // 处理key的时候，要从SelectedKeys集合中删除，否则下次处理会有问题。
                iter.remove();
                log.debug("key: {}", key);
                // 5. 区分事件类型
                if(key.isAcceptable()){
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();

                    SocketChannel sc = channel.accept();
                    // selectedKeys 会主动添加，不会主动删除，处理完一个key就要把它删除，否则取出来为null
                    sc.configureBlocking(false);
                    SelectionKey scKey = sc.register(selector, 0, null);
                    scKey.interestOps(SelectionKey.OP_READ);
                    log.debug("{}", sc);
                    log.debug("scKey:{}", scKey);
                } else if(key.isReadable()){    // 如果是read
                    try{
                        SocketChannel channel = (SocketChannel) key.channel();  // 拿到触发事件的channel
                        ByteBuffer buffer = ByteBuffer.allocate(4);
                        int read = channel.read(buffer); // 如果是正常断开，read的方法的返回值是-1
                        if(read == -1){
                            key.cancel();
                        } else{
                            buffer.flip();
                            // debugRead(buffer);
                            System.out.println(Charset.defaultCharset().decode(buffer));
                        }
                    } catch(IOException e){
                        e.printStackTrace();
                        key.cancel();   // 由于客户端断开了，因此需要将key取消（从selector的key集合中真正删除）
                    }
                }
                // key.cancel();
            }
        }
    }
}
