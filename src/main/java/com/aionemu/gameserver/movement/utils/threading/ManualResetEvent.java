package com.aionemu.gameserver.movement.utils.threading;

/**
 * 手动复位事件：线程可等待信号，信号需显式复位。
 * Manual-reset event: threads wait for a signal that must be reset explicitly.
 */
public class ManualResetEvent {

	/**
	 * 用于 wait/notify 的监视器对象。
	 * Monitor object used for wait/notify.
	 */
	private final Object monitor = new Object();

	/**
	 * 事件是否处于已触发（开放）状态。
	 * Whether the event is in the signaled (open) state.
	 */
	private volatile boolean open = false;

	/**
	 * 以指定初始状态创建事件。
	 * Create an event with the given initial state.
	 *
	 * @param open 初始是否已触发 / Whether initially signaled
	 */
	public ManualResetEvent(boolean open) {
		this.open = open;
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	/**
	 * 阻塞直到事件被触发。
	 * Block until the event is signaled.
	 *
	 * @throws InterruptedException 等待被中断时 / if the wait is interrupted
	 */
	public void waitOne() throws InterruptedException {
		Object object = this.monitor;
		synchronized (object) {
			while (!this.open) {
				this.monitor.wait();
			}
		}
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	/**
	 * 先复位再阻塞等待下一次触发。
	 * Reset first, then block until the next signal.
	 *
	 * @throws InterruptedException 等待被中断时 / if the wait is interrupted
	 */
	public void resetAndWaitOne() throws InterruptedException {
		Object object = this.monitor;
		synchronized (object) {
			this.open = false;
			while (!this.open) {
				this.monitor.wait();
			}
		}
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	/**
	 * 在超时内等待事件触发。
	 * Wait for the event to be signaled within a timeout.
	 *
	 * @param milliseconds 超时毫秒数 / timeout in milliseconds
	 * @return 触发成功为 true，超时为 false / {@code true} if signaled, {@code false} on timeout
	 * @throws InterruptedException 等待被中断时 / if the wait is interrupted
	 */
	public boolean waitOne(long milliseconds) throws InterruptedException {
		Object object = this.monitor;
		synchronized (object) {
			if (this.open) {
				return true;
			}
			this.monitor.wait(milliseconds);
			return this.open;
		}
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	/**
	 * 触发事件并唤醒全部等待线程。
	 * Signal the event and wake all waiting threads.
	 */
	public void set() {
		Object object = this.monitor;
		synchronized (object) {
			this.open = true;
			this.monitor.notifyAll();
		}
	}

	/**
	 * 将事件复位为未触发状态。
	 * Reset the event to the non-signaled state.
	 */
	public void reset() {
		this.open = false;
	}
}
