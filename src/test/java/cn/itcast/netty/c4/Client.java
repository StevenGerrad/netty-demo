package cn.itcast.netty.c4;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Client {
    public static void main(String[] args) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost", 8080));
        // sc.write(Charset.defaultCharset().encode("hello!"));
        // SocketAddress address = sc.getLocalAddress();
        // sc.write(Charset.defaultCharset().encode("123456789132456789/4561312\n"));
        sc.write(Charset.defaultCharset().encode("1234567890abcdef"));
        System.out.println("waiting...");
    }

}

