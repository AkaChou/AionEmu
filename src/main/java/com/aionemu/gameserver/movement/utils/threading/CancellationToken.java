/*
 * Decompiled with CFR 0.150.
 */
package com.aionemu.gameserver.movement.utils.threading;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class CancellationToken {
	private final AtomicBoolean _isCancelled;
	private final Queue<Runnable> _cancelActions = new ConcurrentLinkedQueue<Runnable>();

	public CancellationToken() {
		this._isCancelled = new AtomicBoolean(false);
	}

	public void cancel() throws InterruptedException {
		if (this._isCancelled.compareAndSet(false, true)) {
			Runnable run;
			while ((run = (Runnable) this._cancelActions.poll()) != null) {
				run.run();
			}
		}
	}

	public void addAction(Runnable runnable) throws InterruptedException {
		if (!this._isCancelled.get()) {
			this._cancelActions.add(runnable);
			if (this._isCancelled.get() && this._cancelActions.remove(runnable)) {
				runnable.run();
			}
		}
	}

	public boolean isCancelled() {
		return this._isCancelled.get();
	}
}
