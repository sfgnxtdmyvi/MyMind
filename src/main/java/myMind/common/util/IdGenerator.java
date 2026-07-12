package myMind.common.util;

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
        // 同一毫秒内，序列号递增
        if (timestamp == lastTimestamp) {
            // 当 sequence 比 MAX_SEQUENCE 大1时，结果为0，即序列号用尽
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                // 等待下一毫秒
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;

        // (时间戳 - 起始时间) << 序列号位数 | 序列号
        return ((timestamp - EPOCH) << SEQUENCE_BITS) | sequence;
    }

}