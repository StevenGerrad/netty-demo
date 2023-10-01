package cn.itcast.netty.c4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static cn.itcast.netty.c4.TestByteBuf.log;

public class TestSlice {
    public static void main(String[] args) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(10);
        buf.writeBytes(new byte[]{'a','b','c','d','e','f','g','h','i','j'});
        log(buf);

        // 在切片过程中，没有发生数据复制
        ByteBuf f1 = buf.slice(0, 5);
        f1.retain();    // 让引用计数加1
        // 切片时会对最大容量做出限制
        // 'a','b','c','d','e', 再加 'x' 不行，不允许向切片后写入
        ByteBuf f2 = buf.slice(5, 5);
        f2.retain();
        log(f1);
        log(f2);

        System.out.println("释放原有 byteBuf 内存");
        buf.release();  // 实际是让引用计数减1
        log(f1);

        f1.release();
        f2.release();

        // System.out.println("========================");
        // f1.setByte(0, 'b');
        // log(f1);
        // log(buf);
    }
}
