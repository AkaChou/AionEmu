package com.aionemu.chatserver.utils;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 简化版 ID 工厂：基于 {@link BitSet} 分配递增可用 ID。
 * Simplified ID factory allocating the next free ID via a {@link BitSet}.
 *
 * @author ATracer
 */
public class IdFactory {

    private final BitSet idList = new BitSet();
    private final ReentrantLock lock = new ReentrantLock();
    private AtomicInteger nextMinId = new AtomicInteger(1);

    /**
     * 在锁保护下分配下一个未使用的 ID。
     * Allocate the next unused ID under lock.
     *
     * Newly allocated ID
     */
    public int nextId() {
        try {
            lock.lock();
            int id = idList.nextClearBit(nextMinId.intValue());
            idList.set(id);
            nextMinId.incrementAndGet();
            return id;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 遗留单例访问入口。
     * Legacy singleton access point.
     *
     * Singleton {@link IdFactory}。 / Singleton {@link IdFactory}
     * @deprecated boot 迁移后请使用 Spring Bean / Prefer the Spring bean after boot migration
     */
    @Deprecated(since = "boot-migration")
    public static IdFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 单例持有者。
     * Singleton holder.
     */
    private static final class SingletonHolder {

        private static final IdFactory INSTANCE = new IdFactory();
    }
}
