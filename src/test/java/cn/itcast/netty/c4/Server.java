package cn.itcast.netty.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static cn.itcast.netty.c1.ByteBufferUtil.debugRead;


@Slf4j
public class Server {
    public static void main(String[] args) throws IOException {
        // 使用 nio 来理解非阻塞模式，单线程
        // 0. ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(16);
        // 1. 创建了服务器
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);       // 使用后，下面变为非阻塞的
        // 2. 绑定监听端口
        ssc.bind(new InetSocketAddress(8080));

        // 3. 连接集合
        List<SocketChannel> channels = new ArrayList<>();
        while(true){
            // 4. accept 建立与客户端连接，SocketChannel 用来与客户端通信
            SocketChannel sc = ssc.accept();
            // 阻塞方法，线程停止运行
            // 非阻塞时，线程还会继续运行，如果没有连接建立，sc是null
            if(sc != null){
                log.debug("connecting... {}", sc);
                sc.configureBlocking(false);
                channels.add(sc);
            }
            for(SocketChannel channel : channels){
                // 5. 接受客户端发送的数据
                int read = channel.read(buffer);
                // 阻塞方法，线程停止运行
                // SocketChannel.configureBlocking 设置非阻塞，如果没有读到数据，read返回0
                if(read > 0){
                    buffer.flip();
                    debugRead(buffer);
                    buffer.clear();
                    log.debug("after read.. {}", channel);
                }

            }
        }
    }
}
