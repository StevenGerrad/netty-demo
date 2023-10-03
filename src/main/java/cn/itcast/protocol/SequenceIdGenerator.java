package cn.itcast.protocol;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description TODO:简单累加，不考虑分布式
 */
public abstract class SequenceIdGenerator {
    private static final AtomicInteger id = new AtomicInteger();

    public static int nextId() {
        return id.incrementAndGet();
    }
}
