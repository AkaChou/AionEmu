package com.aionemu.gameserver.movement.utils.threading;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 可取消令牌：支持注册取消回调，并在首次取消时顺序执行。
 * Cancellation token that registers cancel callbacks and runs them once on first cancel.
 */
public class CancellationToken {

	/**
	 * 是否已取消。
	 * Whether cancellation has been requested.
	 */
	private final AtomicBoolean _isCancelled;

	/**
	 * 取消时待执行的回调队列。
	 * Queue of callbacks to run on cancellation.
	 */
	private final Queue<Runnable> _cancelActions = new ConcurrentLinkedQueue<Runnable>();

	/**
	 * 创建未取消状态的令牌。
	 * Create a token in the non-cancelled state.
	 */
	public CancellationToken() {
		this._isCancelled = new AtomicBoolean(false);
	}

	/**
	 * 请求取消：仅首次生效，并依次执行已注册回调。
	 * Request cancellation: takes effect only once and runs registered callbacks in order.
	 *
	 * If a callback is interrupted。 / If a callback is interrupted.
	 */
	public void cancel() throws InterruptedException {
		if (this._isCancelled.compareAndSet(false, true)) {
			Runnable run;
			while ((run = (Runnable) this._cancelActions.poll()) != null) {
				run.run();
			}
		}
	}

	/**
	 * 注册取消回调；若令牌已取消则立即执行该回调。
	 * Register a cancel callback; run it immediately if the token is already cancelled.
	 *
	 * Cancellation callback
	 *
	 * @param runnable @throws InterruptedException 立即执行回调被中断时 / If the immediate callback is interrupted
	 */
	public void addAction(Runnable runnable) throws InterruptedException {
		if (!this._isCancelled.get()) {
			this._cancelActions.add(runnable);
			if (this._isCancelled.get() && this._cancelActions.remove(runnable)) {
				runnable.run();
			}
		}
	}

	/**
	 * 返回令牌是否已取消。
	 * Return whether the token has been cancelled.
	 *
	 * @return {@code true} if cancelled。 / {@code true} if cancelled
	 */
	public boolean isCancelled() {
		return this._isCancelled.get();
	}
}
