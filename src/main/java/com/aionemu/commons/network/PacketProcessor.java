package com.aionemu.commons.network;

import com.aionemu.commons.network.packet.BaseClientPacket;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据包处理器,负责管理和处理客户端数据包
 * Packet processor responsible for managing and processing client packets
 *
 * 该类实现了以下功能:
 * This class implements the following features:
 * 1. 动态线程池管理 / Dynamic thread pool management
 * 2. 数据包队列处理 / Packet queue processing
 * 3. 负载自适应 / Load adaptation
 * 4. 线程安全的数据包处理 / Thread-safe packet processing
 *
 * @param <T> 连接类型 / Connection type
 */
public class PacketProcessor<T extends AConnection> {
    
    /**
     * 日志记录器
     * Logger for PacketProcessor
     */
    private static final Logger log = LoggerFactory.getLogger(PacketProcessor.class.getName());
    
    /**
     * 线程创建阈值
     * Thread spawn threshold
     */
    private final int threadSpawnThreshold;
    
    /**
     * 线程销毁阈值
     * Thread kill threshold
     */
    private final int threadKillThreshold;
    
    /**
     * 用于同步的锁
     * Lock for synchronization
     */
    private final Lock lock;
    
    /**
     * 数据包队列非空条件
     * Condition for non-empty packet queue
     */
    private final Condition notEmpty;
    
    /**
     * 数据包队列
     * Packet queue
     */
    private final List<BaseClientPacket<T>> packets;
    
    /**
     * 处理线程列表
     * Processing thread list
     */
    private final List<Thread> threads;
    
    /**
     * 最小线程数
     * Minimum number of threads
     */
    private final int minThreads;
    
    /**
     * 最大线程数
     * Maximum number of threads
     */
    private final int maxThreads;
    
    /**
     * 数据包执行器
     * Packet executor
     */
    private final Executor executor;

    /**
     * 构造函数,使用默认执行器
     * Constructor with default executor
     */
    public PacketProcessor(int minThreads, int maxThreads, int threadSpawnThreshold, int threadKillThreshold) {
        this(minThreads, maxThreads, threadSpawnThreshold, threadKillThreshold, new DummyExecutor());
    }

    /**
     * 构造函数
     * Constructor
     *
     * @param minThreads 最小线程数 / Minimum number of threads
     * @param maxThreads 最大线程数 / Maximum number of threads
     * @param threadSpawnThreshold 线程创建阈值 / Thread spawn threshold
     * @param threadKillThreshold 线程销毁阈值 / Thread kill threshold
     * @param executor 数据包执行器 / Packet executor
     */
    public PacketProcessor(int minThreads, int maxThreads, int threadSpawnThreshold, int threadKillThreshold, Executor executor) {
        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        this.packets = new LinkedList<BaseClientPacket<T>>();
        this.threads = new ArrayList<Thread>();
        
        Preconditions.checkArgument(minThreads > 0, "Min Threads must be positive");
        Preconditions.checkArgument(maxThreads >= minThreads, "Max Threads must be >= Min Threads");
        Preconditions.checkArgument(threadSpawnThreshold > 0, "Thread Spawn Threshold must be positive");
        Preconditions.checkArgument(threadKillThreshold > 0, "Thread Kill Threshold must be positive");
        
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.threadSpawnThreshold = threadSpawnThreshold;
        this.threadKillThreshold = threadKillThreshold;
        this.executor = executor;
        
        if (minThreads != maxThreads) {
            this.startCheckerThread();
        }

        for (int i = 0; i < minThreads; i++) {
            this.newThread();
        }
    }

    /**
     * 启动检查线程
     * Start checker thread
     */
    private void startCheckerThread() {
        new Thread(new CheckerTask(), "PacketProcessor:Checker").start();
    }

    /**
     * 创建新的处理线程
     * Create new processing thread
     *
     * @return 是否成功创建线程 / Whether thread creation succeeded
     */
    private boolean newThread() {
        if (this.threads.size() >= this.maxThreads) {
            return false;
        }
        
        String name = "PacketProcessor:" + this.threads.size();
        log.debug("Creating new PacketProcessor Thread: " + name);
        Thread t = new Thread(new PacketProcessorTask(), name);
        this.threads.add(t);
        t.start();
        return true;
    }

    /**
     * 终止一个处理线程
     * Terminate a processing thread
     */
    private void killThread() {
        if (this.threads.size() > this.minThreads) {
            Thread t = this.threads.remove(this.threads.size() - 1);
            log.debug("Killing PacketProcessor Thread: " + t.getName());
            t.interrupt();
        }
    }

    /**
     * 执行数据包
     * Execute packet
     *
     * @param packet 要执行的数据包 / Packet to execute
     */
    public final void executePacket(BaseClientPacket<T> packet) {
        this.lock.lock();
        try {
            this.packets.add(packet);
            this.notEmpty.signal();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * 获取第一个可用的数据包
     * Get first available packet
     *
     * @return 可用的数据包 / Available packet
     */
    private BaseClientPacket<T> getFirstAviable() {
        while (true) {
            if (this.packets.isEmpty()) {
                this.notEmpty.awaitUninterruptibly();
            } else {
                ListIterator<BaseClientPacket<T>> it = this.packets.listIterator();
                
                while (it.hasNext()) {
                    BaseClientPacket<T> packet = it.next();
                    if (packet.getConnection().tryLockConnection()) {
                        it.remove();
                        return packet;
                    }
                }
                
                this.notEmpty.awaitUninterruptibly();
            }
        }
    }

    /**
     * 检查任务,用于动态调整线程池大小
     * Checker task for dynamically adjusting thread pool size
     */
    private final class CheckerTask implements Runnable {
        private static final int sleepTime = 60000;
        private int lastSize;

        private CheckerTask() {
            this.lastSize = 0;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // Ignore interruption
            }

            int packetsToExecute = packets.size();
            if (packetsToExecute < lastSize && packetsToExecute < threadKillThreshold) {
                killThread();
            } else if (packetsToExecute > lastSize && packetsToExecute > threadSpawnThreshold 
                    && !newThread() && packetsToExecute >= threadSpawnThreshold * 3) {
                log.info("Lag detected! [" + packetsToExecute + " client packets are waiting for execution]. "
                        + "Consider increasing PacketProcessor maxThreads or hardware upgrade.");
            }

            lastSize = packetsToExecute;
        }
    }

    /**
     * 数据包处理任务
     * Packet processing task
     */
    private final class PacketProcessorTask implements Runnable {
        @Override
        public void run() {
            BaseClientPacket<T> packet = null;

            while (true) {
                lock.lock();
                try {
                    if (packet != null) {
                        packet.getConnection().unlockConnection();
                    }

                    if (Thread.interrupted()) {
                        return;
                    }

                    packet = getFirstAviable();
                } finally {
                    lock.unlock();
                }

                executor.execute(packet);
            }
        }
    }

    /**
     * 默认执行器实现
     * Default executor implementation
     */
    private static class DummyExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
