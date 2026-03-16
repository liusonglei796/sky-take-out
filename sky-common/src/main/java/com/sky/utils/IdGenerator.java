package com.sky.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 雪花算法 ID 生成器
 * 用于生成全局唯一的订单号等
 */
public class IdGenerator {

    // 起始时间戳 (2024-01-01)
    private final long twepoch = 1704067200000L;

    // 机器标识占用的位数
    private final long workerIdBits = 5L;
    // 数据中心标识占用的位数
    private final long datacenterIdBits = 5L;
    // 序列号占用的位数
    private final long sequenceBits = 12L;

    // 机器标识最大值 31
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    // 数据中心标识最大值 31
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    // 机器标识向左移 12 位
    private final long workerIdShift = sequenceBits;
    // 数据中心标识向左移 17 位
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    // 时间戳向左移 22 位
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    // 生成序列的掩码 4095
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private static volatile IdGenerator instance;

    public static IdGenerator getInstance() {
        if (instance == null) {
            synchronized (IdGenerator.class) {
                if (instance == null) {
                    instance = new IdGenerator();
                }
            }
        }
        return instance;
    }

    private IdGenerator() {
        this.workerId = getWorkerIdByHost();
        this.datacenterId = 1L; // 默认数据中心
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    private long getWorkerIdByHost() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            byte[] bytes = ip.getAddress();
            long id = 0;
            for (byte b : bytes) {
                id = (id << 8) | (b & 0xFF);
            }
            return id % (maxWorkerId + 1);
        } catch (UnknownHostException e) {
            return 1L;
        }
    }
}
