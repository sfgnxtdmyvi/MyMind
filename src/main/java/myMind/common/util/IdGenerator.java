package myMind.common.util;

import java.util.concurrent.locks.LockSupport;

public class IdGenerator {

    // 序列号位数
    private static final long SEQUENCE_BITS = 4L;
    // 等价于：2^SEQUENCE_BITS - 1
    // 左移 SEQUENCE_BITS 位，再按位取反
    // -1L：11111
    // -1L << 3：左移 4 位，低位补 0。结果为 10000
    // ~(...)：按位取反，01111，即 2^4 - 1 = 15
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // 起始时间戳，用来减少 id 的位数
    private static final long EPOCH = 1782572984421L;

    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    public static synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨
        if (timestamp < lastTimestamp) {
            // 回拨幅度
            long offset = lastTimestamp - timestamp;

            if (offset <= 5) {
                // 轻微(<5ms)：短暂自旋等待，精度高
                timestamp = waitUntilNextMillis(lastTimestamp);
            } else if (offset <= 100) {
                // 中度(5~100ms)：让出CPU，parkNanos等待
                LockSupport.parkNanos(offset * 1_000_000L);
                timestamp = System.currentTimeMillis();
            } else {
                // 严重(>100ms)：抛异常/报警，拒绝服务
                throw new RuntimeException("Clock moved backwards by " + offset + "ms");
            }
        }

        // 同一毫秒内，序列号递增
        if (timestamp == lastTimestamp) {
            // 当 sequence 比 MAX_SEQUENCE 大1时，结果为0，即序列号用尽
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                // 等待下一毫秒
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;

        // (时间戳 - 起始时间) << 序列号位数 | 序列号
        return ((timestamp - EPOCH) << SEQUENCE_BITS) | sequence;
    }

    // 等待时间最多只有 1 毫秒，sleep 精度不够，本该等 1ms 却可能睡 1~15ms
    // sleep 会触发线程上下文切换，开销反而更大
    private static long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

}