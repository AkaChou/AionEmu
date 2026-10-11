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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;

/**
 * 客户端数据包处理器：连接亲和条带化队列调度、连接互斥与自适应线程池。
 * Client packet processor: connection-affinity striped queue scheduling, connection mutex, and adaptive thread pool.
 *
 * @param <T> 连接类型 / Connection type
 */
@Slf4j
public class PacketProcessor<T extends AConnection> {

	/**
	 * 默认队列分片条带数（必须为 2 的幂次方以加速位运算取模）。
	 * Default queue stripe count (must be a power of two for fast bitwise modulo).
	 */
	private static final int DEFAULT_STRIPES = 8;

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
	 * 全局协调互斥锁（仅在所有分片均无可用数据包时用于工作线程等待与唤醒）。
	 * Global coordination mutex lock used only for worker wait/signal when all stripes are idle.
	 */
	private final Lock lock;

	/**
	 * 队列非空协调条件变量。
	 * Non-empty queue coordination condition.
	 */
	private final Condition notEmpty;

	/**
	 * 兼容保留字段。
	 * Compatibility preserved field.
	 */
	private final LinkedList<BaseClientPacket<T>> packets;

	/**
	 * 连接亲和分片条带数组。
	 * Connection-affinity striped queue array.
	 */
	private final QueueStripe<T>[] stripes;

	/**
	 * 当前队列积压数据包总数。
	 * Total pending packets count across all stripes.
	 */
	private final AtomicInteger totalPackets = new AtomicInteger();

	/**
	 * 当前处于等待状态的工作线程数。
	 * Count of worker threads currently waiting for packets.
	 */
	private final AtomicInteger waitingWorkers = new AtomicInteger();

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
	 * @param minThreads           最小线程数 / Minimum threads
	 * @param maxThreads           最大线程数 / Maximum threads
	 * @param threadSpawnThreshold 线程创建阈值 / Spawn threshold
	 * @param threadKillThreshold  线程销毁阈值 / Kill threshold
	 */
	public PacketProcessor(int minThreads, int maxThreads, int threadSpawnThreshold, int threadKillThreshold) {
		this(minThreads, maxThreads, threadSpawnThreshold, threadKillThreshold, new DummyExecutor());
	}

	/**
	 * 使用指定执行器与默认线程工厂构造。
	 * Construct with executor and default thread factory.
	 *
	 * @param minThreads           最小线程数 / Minimum threads
	 * @param maxThreads           最大线程数 / Maximum threads
	 * @param threadSpawnThreshold 线程创建阈值 / Spawn threshold
	 * @param threadKillThreshold  线程销毁阈值 / Kill threshold
	 * @param executor             数据包执行器 / Packet executor
	 */
	public PacketProcessor(int minThreads, int maxThreads, int threadSpawnThreshold, int threadKillThreshold, Executor executor) {
		this(minThreads, maxThreads, threadSpawnThreshold, threadKillThreshold, executor,
			new PriorityThreadFactory("PacketProcessor", Thread.NORM_PRIORITY));
	}

	/**
	 * 完整构造并启动初始工作线程。
	 * Full constructor that starts initial worker threads.
	 *
	 * @param minThreads           最小线程数 / Minimum threads
	 * @param maxThreads           最大线程数 / Maximum threads
	 * @param threadSpawnThreshold 线程创建阈值 / Spawn threshold
	 * @param threadKillThreshold  线程销毁阈值 / Kill threshold
	 * @param executor             数据包执行器 / Packet executor
	 * @param threadFactory        线程工厂 / Thread factory
	 */
	@SuppressWarnings("unchecked")
	PacketProcessor(int minThreads, int maxThreads, int threadSpawnThreshold, int threadKillThreshold, Executor executor, ThreadFactory threadFactory) {
		this.lock = new ReentrantLock();
		this.notEmpty = this.lock.newCondition();
		this.packets = new LinkedList<>();
		this.threads = new ArrayList<>();

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

		this.stripes = new QueueStripe[DEFAULT_STRIPES];
		for (int i = 0; i < DEFAULT_STRIPES; i++) {
			this.stripes[i] = new QueueStripe<>();
		}

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
		log.debug("Creating new PacketProcessor Thread: {}", name);
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
			Thread t = this.threads.removeLast();
			log.debug("Killing PacketProcessor Thread: {}", t.getName());
			t.interrupt();
		}
	}

	/**
	 * 根据连接对象哈希值确定所属分片条带。
	 * Determines the target stripe index based on the connection hash code.
	 *
	 * @param connection 网络连接 / Network connection
	 * @return 分片索引 / Stripe index
	 */
	private int stripeIndex(T connection) {
		int h = connection != null ? connection.hashCode() : 0;
		h ^= (h >>> 16);
		return h & (this.stripes.length - 1);
	}

	/**
	 * 将数据包路由至连接亲和分片并唤醒等待中的工作线程。
	 * Enqueue packet to connection-affinity stripe and signal waiting workers.
	 *
	 * @param packet 客户端数据包 / Client packet
	 */
	public final void executePacket(BaseClientPacket<T> packet) {
		T connection = packet.getConnection();
		int idx = stripeIndex(connection);
		QueueStripe<T> stripe = this.stripes[idx];

		stripe.lock.lock();
		try {
			stripe.packets.add(packet);
			this.totalPackets.incrementAndGet();
		} finally {
			stripe.lock.unlock();
		}

		signalNotEmpty();
	}

	/**
	 * 唤醒可能处于空闲等待状态的工作线程。
	 * Signals waiting worker threads if any.
	 */
	private void signalNotEmpty() {
		this.lock.lock();
		try {
			this.notEmpty.signal();
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * 轮询各分片获取第一个可锁定连接的数据包。
	 * Round-robin polls stripes to get the first packet whose connection can be locked.
	 *
	 * @param cursor 当前工作线程的分片轮转游标 / Stripe round-robin cursor for the worker
	 * @return 可用数据包 / Available packet
	 * @throws InterruptedException 等待被中断 / Wait interrupted
	 */
	private BaseClientPacket<T> getFirstAvailable(int[] cursor) throws InterruptedException {
		while (true) {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}

			int startIdx = cursor[0];
			int len = this.stripes.length;

			for (int i = 0; i < len; i++) {
				int idx = (startIdx + i) & (len - 1);
				QueueStripe<T> stripe = this.stripes[idx];

				if (stripe.lock.tryLock()) {
					try {
						if (!stripe.packets.isEmpty()) {
							BaseClientPacket<T> first = stripe.packets.peekFirst();
							if (first != null && first.getConnection().tryLockConnection()) {
								stripe.packets.pollFirst();
								this.totalPackets.decrementAndGet();
								cursor[0] = (idx + 1) & (len - 1);
								return first;
							}

							if (stripe.packets.size() > 1) {
								ListIterator<BaseClientPacket<T>> it = stripe.packets.listIterator(1);
								while (it.hasNext()) {
									BaseClientPacket<T> packet = it.next();
									if (packet.getConnection().tryLockConnection()) {
										it.remove();
										this.totalPackets.decrementAndGet();
										cursor[0] = (idx + 1) & (len - 1);
										return packet;
									}
								}
							}
						}
					} finally {
						stripe.lock.unlock();
					}
				}
			}

			// 全部分片暂时没有可用数据包，进入全局协调等待
			this.lock.lock();
			try {
				if (this.totalPackets.get() == 0) {
					this.waitingWorkers.incrementAndGet();
					try {
						this.notEmpty.await();
					} finally {
						this.waitingWorkers.decrementAndGet();
					}
				}
			} finally {
				this.lock.unlock();
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

				int packetsToExecute = totalPackets.get();
				if (packetsToExecute < lastSize && packetsToExecute < threadKillThreshold) {
					killThread();
				} else if (packetsToExecute > lastSize && packetsToExecute > threadSpawnThreshold
					&& !newThread() && packetsToExecute >= threadSpawnThreshold * 3) {
					log.info(I18n.get("log.9937589c3369", packetsToExecute));
				}

				lastSize = packetsToExecute;
			}
		}
	}

	/**
	 * 数据包处理任务：取包、执行并在分片锁保护下释放连接互斥锁。
	 * Packet processing task: take packet, execute, and unlock connection under stripe lock.
	 */
	private final class PacketProcessorTask implements Runnable {
		@Override
		public void run() {
			BaseClientPacket<T> packet = null;
			int[] cursor = new int[]{0};

			while (true) {
				if (packet != null) {
					T connection = packet.getConnection();
					int idx = stripeIndex(connection);
					QueueStripe<T> stripe = stripes[idx];
					stripe.lock.lock();
					try {
						connection.unlockConnection();
					} finally {
						stripe.lock.unlock();
					}
					signalNotEmpty();
				}

				if (Thread.interrupted()) {
					return;
				}

				try {
					packet = getFirstAvailable(cursor);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}

				executor.execute(packet);
			}
		}
	}

	/**
	 * 队列分片条带：拥有独立的互斥锁与双向链表队列。
	 * Queue stripe with independent mutex lock and doubly-linked queue.
	 */
	private static final class QueueStripe<T extends AConnection> {
		private final Lock lock = new ReentrantLock();
		private final LinkedList<BaseClientPacket<T>> packets = new LinkedList<>();
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
