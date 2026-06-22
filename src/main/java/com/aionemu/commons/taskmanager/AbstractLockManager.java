package com.aionemu.commons.taskmanager;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * 抽象锁管理器类，提供基础的读写锁控制功能
 * Abstract Lock Manager class that provides basic read-write lock control functionality
 * 
 * 该类使用ReentrantReadWriteLock实现线程安全的读写操作
 * This class uses ReentrantReadWriteLock to implement thread-safe read and write operations
 */
public abstract class AbstractLockManager {

    /**
     * 可重入读写锁实例
     * ReentrantReadWriteLock instance
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 写锁实例
     * Write lock instance
     */
    private final WriteLock writeLock;

    /**
     * 读锁实例
     * Read lock instance
     */
    private final ReadLock readLock;

    /**
     * 构造函数，初始化读写锁
     * Constructor, initializes read and write locks
     */
    public AbstractLockManager() {
        this.writeLock = this.lock.writeLock();
        this.readLock = this.lock.readLock();
    }

    /**
     * 获取写锁
     * Acquire write lock
     */
    public final void writeLock() {
        this.writeLock.lock();
    }

    /**
     * 释放写锁
     * Release write lock
     */
    public final void writeUnlock() {
        this.writeLock.unlock();
    }

    /**
     * 获取读锁
     * Acquire read lock
     */
    public final void readLock() {
        this.readLock.lock();
    }

    /**
     * 释放读锁
     * Release read lock
     */
    public final void readUnlock() {
        this.readLock.unlock();
    }
}
