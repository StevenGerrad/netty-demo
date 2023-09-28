package cn.itcast.netty.c1;

import java.nio.ByteBuffer;

public class TestByteBufferAllocate {
    public static void main(String[] args){
        System.out.println(ByteBuffer.allocate(16).getClass());
        System.out.println(ByteBuffer.allocateDirect(16).getClass());
        /*
        * class java.nio.HeapByteBuffer     - java 堆内存，读写效率低，分配快，受 GC 影响
        * class java.nio.DirectByteBuffer   - 直接内存，读写效率高（少一次拷贝），分配慢，受 GC 影响
        * */
    }
}
