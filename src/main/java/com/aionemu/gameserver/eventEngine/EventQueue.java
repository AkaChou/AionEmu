package com.aionemu.gameserver.eventEngine;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 延迟事件阻塞优先队列：按 {@link DelayedEvent#compareTo} 排序，仅在到期后可取出。
 * Blocking priority queue of delayed events ordered by {@link DelayedEvent#compareTo}; only due items poll.
 *
 * @param <E> 延迟事件类型 / delayed event type
 * @author wanke
 */
public class EventQueue<E extends DelayedEvent> extends AbstractQueue<E> implements BlockingQueue<E> {

	/**
	 * 队列互斥锁。
	 * Queue mutex.
	 */
	private transient final ReentrantLock lock = new ReentrantLock();

	/**
	 * 有新头元素或到期时的等待条件。
	 * Condition signaled when head changes or becomes due.
	 */
	private transient final Condition available = lock.newCondition();

	/**
	 * 内部优先队列。
	 * Backing priority queue.
	 */
	private final PriorityQueue<E> q = new PriorityQueue<E>();

	/**
	 * 空队列。
	 * Empty queue.
	 */
	public EventQueue() {
	}

	/**
	 * 以给定集合初始化。
	 * Initializes from a collection.
	 *
	 * @param c 初始元素 / initial elements
	 */
	public EventQueue(Collection<? extends E> c) {
		this.addAll(c);
	}

	/**
	 * 添加元素（等同 {@link #offer(DelayedEvent)}）。
	 * Adds an element (same as {@link #offer(DelayedEvent)}).
	 *
	 * @param e 元素 / element
	 * always true
	 */
	public boolean add(E e) {
		return offer(e);
	}

	/**
	 * 入队；若成为新头则唤醒等待者。
	 * Enqueues; signals waiters when the element becomes the new head.
	 *
	 * @param e 元素 / element
	 * always true
	 */
	public boolean offer(E e) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			E first = q.peek();
			q.offer(e);
			if (first == null || e.compareTo(first) < 0) {
				available.signalAll();
			}
			return true;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 阻塞入队（实际非阻塞，委托 {@link #offer(DelayedEvent)}）。
	 * Blocking put (actually non-blocking; delegates to {@link #offer(DelayedEvent)}).
	 *
	 * @param e 元素 / element
	 */
	public void put(E e) {
		offer(e);
	}

	/**
	 * 带超时入队（忽略超时，委托 {@link #offer(DelayedEvent)}）。
	 * Timed offer (timeout ignored; delegates to {@link #offer(DelayedEvent)}).
	 *
	 * @param e 元素 / element
	 * timeout
	 * @param unit 时间单位 / time unit
	 * always true
	 */
	public boolean offer(E e, long timeout, TimeUnit unit) {
		return offer(e);
	}

	/**
	 * 非阻塞取出已到期头元素；未到期或空则返回 null。
	 * Non-blocking poll of a due head; returns null if empty or not yet due.
	 *
	 * @return 到期元素或 null / due element or null
	 */
	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			E first = q.peek();
			if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0) {
				return null;
			} else {
				E x = q.poll();
				assert x != null;
				if (q.size() != 0) {
					available.signalAll();
				}
				return x;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 阻塞直到有到期元素可取。
	 * Blocks until a due element is available.
	 *
	 * due element
	 *
	 * @return @throws InterruptedException 等待被中断 / wait interrupted
	 */
	public E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			for (;;) {
				E first = q.peek();
				if (first == null) {
					available.await();
				} else {
					long delay = first.getDelay(TimeUnit.NANOSECONDS);
					if (delay > 0) {
						@SuppressWarnings("unused")
						long tl = available.awaitNanos(delay);
					} else {
						E x = q.poll();
						assert x != null;
						if (q.size() != 0) {
							available.signalAll();
						}
						return x;
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 在超时内阻塞取出到期元素。
	 * Timed blocking poll of a due element.
	 *
	 * timeout
	 *
	 * @param unit 时间单位 / time unit
	 * @param unit @return 到期元素或 null / due element or null
	 * @return @throws InterruptedException 等待被中断 / wait interrupted
	 */
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			for (;;) {
				E first = q.peek();
				if (first == null) {
					if (nanos <= 0) {
						return null;
					} else {
						nanos = available.awaitNanos(nanos);
					}
				} else {
					long delay = first.getDelay(TimeUnit.NANOSECONDS);
					if (delay > 0) {
						if (nanos <= 0) {
							return null;
						}
						if (delay > nanos) {
							delay = nanos;
						}
						long timeLeft = available.awaitNanos(delay);
						nanos -= delay - timeLeft;
					} else {
						E x = q.poll();
						assert x != null;
						if (q.size() != 0) {
							available.signalAll();
						}
						return x;
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 查看头元素（不移除，不论是否到期）。
	 * Peeks head without removal (regardless of due time).
	 *
	 * head or null
	 */
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.peek();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 当前元素数。
	 * Current size.
	 *
	 * size
	 */
	public int size() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.size();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 将所有已到期元素转移到目标集合。
	 * Drains all due elements into the target collection.
	 *
	 * @param c 目标集合 / target collection
	 * transferred count
	 */
	public int drainTo(Collection<? super E> c) {
		if (c == null) {
			throw new NullPointerException();
		}
		if (c == this) {
			throw new IllegalArgumentException();
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = 0;
			for (;;) {
				E first = q.peek();
				if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0) {
					break;
				}
				c.add(q.poll());
				++n;
			}
			if (n > 0) {
				available.signalAll();
			}
			return n;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 将至多 {@code maxElements} 个已到期元素转移到目标集合。
	 * Drains up to {@code maxElements} due elements into the target collection.
	 *
	 * @param c 目标集合 / target collection
	 * max count
	 * transferred count
	 */
	public int drainTo(Collection<? super E> c, int maxElements) {
		if (c == null) {
			throw new NullPointerException();
		}
		if (c == this) {
			throw new IllegalArgumentException();
		}
		if (maxElements <= 0) {
			return 0;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = 0;
			while (n < maxElements) {
				E first = q.peek();
				if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0) {
					break;
				}
				c.add(q.poll());
				++n;
			}
			if (n > 0) {
				available.signalAll();
			}
			return n;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 清空队列。
	 * Clears the queue.
	 */
	public void clear() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			q.clear();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 剩余容量（无界，返回 {@link Integer#MAX_VALUE}）。
	 * Remaining capacity (unbounded, returns {@link Integer#MAX_VALUE}).
	 *
	 * remaining capacity
	 */
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	/**
	 * 快照数组。
	 * Snapshot array.
	 *
	 * element array
	 */
	public Object[] toArray() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.toArray();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 快照到给定数组。
	 * Snapshot into the given array.
	 *
	 * @param a 目标数组 / target array
	 * @param <T> 数组元素类型 / array element type
	 * @return 填充后的数组 / filled array
	 */
	public <T> T[] toArray(T[] a) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.toArray(a);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 移除指定对象。
	 * Removes the given object.
	 *
	 * @param o 对象 / object
	 * whether removed
	 */
	public boolean remove(Object o) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return q.remove(o);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 基于快照的迭代器。
	 * Snapshot-based iterator.
	 *
	 * iterator
	 */
	public Iterator<E> iterator() {
		return new Itr(toArray());
	}

	/**
	 * 快照迭代器；{@link #remove()} 按引用从真实队列删除。
	 * Snapshot iterator; {@link #remove()} deletes by reference from the live queue.
	 */
	private class Itr implements Iterator<E> {

		/**
		 * 快照数组。
		 * Snapshot array.
		 */
		final Object[] array;

		/**
		 * 当前游标。
		 * Current cursor.
		 */
		int cursor;

		/**
		 * 最近返回下标；-1 表示不可 remove。
		 * Last returned index; -1 means remove is illegal.
		 */
		int lastRet;

		/**
		 * 快照 / snapshot
		 */
		Itr(Object[] array) {
			lastRet = -1;
			this.array = array;
		}

		public boolean hasNext() {
			return cursor < array.length;
		}

		@SuppressWarnings("unchecked")
		public E next() {
			if (cursor >= array.length) {
				throw new NoSuchElementException();
			}
			lastRet = cursor;
			return (E) array[cursor++];
		}

		public void remove() {
			if (lastRet < 0) {
				throw new IllegalStateException();
			}
			Object x = array[lastRet];
			lastRet = -1;
			lock.lock();
			try {
				for (@SuppressWarnings("rawtypes")
				Iterator it = q.iterator(); it.hasNext();) {
					if (it.next() == x) {
						it.remove();
						return;
					}
				}
			} finally {
				lock.unlock();
			}
		}
	}
}
