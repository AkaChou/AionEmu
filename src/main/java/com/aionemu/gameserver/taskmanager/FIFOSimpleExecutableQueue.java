package com.aionemu.gameserver.taskmanager;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * 基于 {@link ArrayDeque} 的简易 FIFO 可执行队列。
 * Simple FIFO executable queue backed by an {@link ArrayDeque}.
 *
 * @param <T> 队列元素类型 / Queue element type
 * @author NB4L1
 */
public abstract class FIFOSimpleExecutableQueue<T> extends FIFOExecutableQueue {

	/**
	 * 内部双端队列。
	 * Internal deque.
	 */
	private final Deque<T> queue = new ArrayDeque<T>();

	/**
	 * 将元素加入队尾并触发执行。
	 * Append an element and trigger execution.
	 *
	 * @param t 元素 / Element
	 */
	public final void execute(T t) {
		synchronized (queue) {
			queue.addLast(t);
		}
		execute();
	}

	/**
	 * 批量加入元素并触发执行。
	 * Append all elements and trigger execution.
	 *
	 * @param c 元素集合 / Element collection
	 */
	public final void executeAll(Collection<T> c) {
		synchronized (queue) {
			queue.addAll(c);
		}
		execute();
	}

	/**
	 * 从队列中移除指定元素。
	 * Remove the given element from the queue.
	 *
	 * @param t 元素 / Element
	 */
	public final void remove(T t) {
		synchronized (queue) {
			queue.remove(t);
		}
	}

	/**
	 * 判断内部队列是否为空（同步保护）。
	 * Whether the internal queue is empty (synchronized).
	 *
	 * @return 若 empty 则为 true / True if empty
	 */
	@Override
	protected final boolean isEmpty() {
		synchronized (queue) {
			return queue.isEmpty();
		}
	}

	/**
	 * 移除并返回队首元素。
	 * Remove and return the first element.
	 *
	 * @return 队首元素 / the first element
	 */
	protected final T removeFirst() {
		synchronized (queue) {
			return queue.removeFirst();
		}
	}

	/**
	 * 取出并执行队首（子类实现具体执行方式）。
	 * Remove and execute the first element (subclass defines how).
	 */
	@Override
	protected abstract void removeAndExecuteFirst();
}
