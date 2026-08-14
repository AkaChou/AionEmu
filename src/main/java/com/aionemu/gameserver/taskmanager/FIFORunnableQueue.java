package com.aionemu.gameserver.taskmanager;

import com.aionemu.commons.utils.concurrent.ExecuteWrapper;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * 以 {@link ExecuteWrapper} 执行 {@link Runnable} 元素的 FIFO 队列。
 * FIFO queue that runs {@link Runnable} elements via {@link ExecuteWrapper}.
 *
 * 元素必须实现 {@link Runnable}。 / Elements must implement {@link Runnable}.
 * @author NB4L1
 */
public abstract class FIFORunnableQueue<T extends Runnable> extends FIFOSimpleExecutableQueue<T> {

	/**
	 * 取出队首并用 {@link ExecuteWrapper} 限时执行。
	 * Dequeue the first runnable and execute it via {@link ExecuteWrapper} with a time limit.
	 */
	@Override
	protected final void removeAndExecuteFirst() {
		ExecuteWrapper.execute(removeFirst(), ThreadPoolManager.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING);
	}
}
