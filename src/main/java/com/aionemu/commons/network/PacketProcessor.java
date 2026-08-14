package com.aionemu.commons.network;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.packet.BaseClientPacket;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.commons.utils.concurrent.PriorityThreadFactory;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端数据包处理器：队列调度、连接互斥与自适应线程池。
 * Client packet processor: queue scheduling, connection mutex, and adaptive thread pool.
 *
 * @param <T> 连接类型 / Connection type
 */
@Slf4j
public class PacketProcessor<T extends AConnection> {

    /**
     * 线程创建阈值。
     * Thread spawn threshold.
     */
    private final int threadSpawnThreshold;

    /**
     * 线程销毁阈值。
     * Thread kill threshold.
     */
    private final int threadKillThreshold;

    /**
     * 同步锁。
     * Synchronization lock.
     */
    private final Lock lock;

    /**
     * 队列非空条件。
     * Non-empty queue condition.
     */
    private final Condition notEmpty;

    /**
     * 数据包队列。
     * Packet queue.
     */
    private final List<BaseClientPacket<T>> packets;

    /**
     * 处理线程列表。
     * Processing thread list.
     */
    private final List<Thread> threads;

    /**
     * 最小线程数。
     * Minimum number of threads.
     */
    private final int minThreads;

    /**
     * 最大线程数。
     * Maximum number of threads.
     */
    private final int maxThreads;

    /**
     * 数据包执行器。
     * Packet executor.
     */
    private final Executor executor;
    private final String serviceContext;
    private final ThreadFactory threadFactory;

    /**
     * 使用默认同步执行器构造。
     * Construct with default synchronous executor.
     *
     * @param minThreads 最小线程数 / Minimum threads
     * @param maxThreads 最大线程数 / Maximum threads
     * @param threadSpawnThreshold 线程创建阈值 / Spawn threshold
     * @param threadKillThreshold 线程销毁阈值 / Kill threshold
     */
    public PacketProcessor(int minThreads, int maxThreads, int threadSpawnThreshold, int threadKillThreshold) {
        this(minThreads, maxThreads, threadSpawnThreshold, threadKillThreshold, new DummyExecutor());
    }

    /**
     * 使用指定执行器与默认线程工厂构造。
     * Construct with executor and default thread factory.
     *
     * @param minThreads 最小线程数 / Minimum threads
     * @param maxThreads 最大线程数 / Maximum threads
     * @param threadSpawnThreshold 线程创建阈值 / Spawn threshold
     * @param threadKillThreshold 线程销毁阈值 / Kill threshold
     * @param executor 数据包执行器 / Packet executor
     */
    public PacketProcessor(int minThreads, int maxThreads, int threadSpawnThreshold, int threadKillThreshold, Executor executor) {
        this(minThreads, maxThreads, threadSpawnThreshold, threadKillThreshold, executor,
            new PriorityThreadFactory("PacketProcessor", Thread.NORM_PRIORITY));
    }

    /**
     * 完整构造并启动初始工作线程。
     * Full constructor that starts initial worker threads.
     *
     * @param minThreads 最小线程数 / Minimum threads
     * @param maxThreads 最大线程数 / Maximum threads
     * @param threadSpawnThreshold 线程创建阈值 / Spawn threshold
     * @param threadKillThreshold 线程销毁阈值 / Kill threshold
     * @param executor 数据包执行器 / Packet executor
     * @param threadFactory 线程工厂 / Thread factory
     */
    PacketProcessor(int minThreads, int maxThreads, int threadSpawnThreshold, int threadKillThreshold, Executor executor, ThreadFactory threadFactory) {
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
        this.threadFactory = Preconditions.checkNotNull(threadFactory, "Thread Factory must not be null");
        this.serviceContext = ServiceContext.current();

        if (minThreads != maxThreads) {
            this.startCheckerThread();
        }

        for (int i = 0; i < minThreads; i++) {
            this.newThread();
        }
    }

    /**
     * 启动自适应检查线程。
     * Start adaptive checker thread.
     */
    private void startCheckerThread() {
        Thread checkerThread = newManagedThread(new CheckerTask(), "PacketProcessor:Checker");
        checkerThread.start();
    }

    /**
     * 创建新的处理线程（不超过上限）。
     * Create a new processing thread if under max.
     *
     * @return 是否创建成功 / Whether creation succeeded
     */
    private boolean newThread() {
        if (this.threads.size() >= this.maxThreads) {
            return false;
        }

        String name = "PacketProcessor:" + this.threads.size();
        log.debug("Creating new PacketProcessor Thread: " + name);
        Thread t = newManagedThread(new PacketProcessorTask(), name);
        this.threads.add(t);
        t.start();
        return true;
    }

    /**
     * 用工厂创建并命名受服务上下文包装的线程。
     * Create and name a service-context-wrapped thread via factory.
     *
     * @param task 任务 / Task
     * @param name 线程名 / Thread name
     * @return 线程 / Thread
     */
    private Thread newManagedThread(Runnable task, String name) {
        Thread thread = threadFactory.newThread(ServiceContext.wrap(task, serviceContext));
        thread.setName(name);
        return thread;
    }

    /**
     * 终止一个多余处理线程。
     * Terminate one excess processing thread.
     */
    private void killThread() {
        if (this.threads.size() > this.minThreads) {
            Thread t = this.threads.remove(this.threads.size() - 1);
            log.debug("Killing PacketProcessor Thread: " + t.getName());
            t.interrupt();
        }
    }

    /**
     * 将数据包入队并唤醒工作线程。
     * Enqueue packet and signal a worker.
     *
     * @param packet 客户端数据包 / Client packet
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
     * 获取第一个可锁定连接的数据包。
     * Get first packet whose connection can be locked.
     *
     * @return 可用数据包 / Available packet
     * @throws InterruptedException 等待被中断 / Wait interrupted
     */
    private BaseClientPacket<T> getFirstAviable() throws InterruptedException {
        while (true) {
            if (this.packets.isEmpty()) {
                this.notEmpty.await();
            } else {
                ListIterator<BaseClientPacket<T>> it = this.packets.listIterator();

                while (it.hasNext()) {
                    BaseClientPacket<T> packet = it.next();
                    if (packet.getConnection().tryLockConnection()) {
                        it.remove();
                        return packet;
                    }
                }

                this.notEmpty.await();
            }
        }
    }

    /**
     * 检查任务：按队列长度动态调整线程数。
     * Checker task that adapts thread count by queue size.
     */
    private final class CheckerTask implements Runnable {
        private static final int sleepTime = 60000;
        private int lastSize;

        private CheckerTask() {
            this.lastSize = 0;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                lock.lock();
                try {
                    int packetsToExecute = packets.size();
                    if (packetsToExecute < lastSize && packetsToExecute < threadKillThreshold) {
                        killThread();
                    } else if (packetsToExecute > lastSize && packetsToExecute > threadSpawnThreshold
                        && !newThread() && packetsToExecute >= threadSpawnThreshold * 3) {
                        log.info(I18n.get("log.9937589c3369", packetsToExecute));
                    }

                    lastSize = packetsToExecute;
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 数据包处理任务：取包、执行并释放连接锁。
     * Packet processing task: take packet, execute, and unlock connection.
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    lock.unlock();
                }

                executor.execute(packet);
            }
        }
    }

    /**
     * 默认同步执行器。
     * Default synchronous executor.
     */
    private static class DummyExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
