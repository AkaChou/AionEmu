package com.aionemu.gameserver.ai2.event;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * AI 事件日志队列，以有界双端队列记录最近的 {@link AIEventType}，满容时丢弃最旧事件。
 * AI event log queue that records recent {@link AIEventType} values in a bounded deque, dropping the oldest when full.
 *
 * @author ATracer
 */
public class AIEventLog extends LinkedBlockingDeque<AIEventType> {

	private static final long serialVersionUID = -7234174243343636729L;

	/**
	 * 使用默认无界容量构造事件日志。
	 * Construct an event log with the default unbounded capacity.
	 */
	public AIEventLog() {
		super();
	}

	/**
	 * 使用指定容量构造有界事件日志。
	 * Construct a bounded event log with the given capacity.
	 *
	 * Queue capacity
	 */
	public AIEventLog(int capacity) {
		super(capacity);
	}

	/**
	 * 将事件插入队列头部；若已满则先移除队尾最旧事件。
	 * Insert an event at the head; if full, remove the oldest event at the tail first.
	 *
	 * @param e 要记录的 AI 事件类型 / AI event type to record
	 * @return Always {@code true}。 / Always {@code true}
	 */
	@Override
	public synchronized boolean offerFirst(AIEventType e) {
		if (remainingCapacity() == 0) {
			removeLast();
		}
		super.offerFirst(e);
		return true;
	}
}
