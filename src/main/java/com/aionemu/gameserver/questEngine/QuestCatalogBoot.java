package com.aionemu.gameserver.questEngine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import com.aionemu.gameserver.questEngine.definition.QuestCatalog;

/**
 * 生产目录预装载状态机：持有 raw catalog 的后台编译 Future，装载时消费、复位时丢弃。
 * Production catalog preload state machine: holds the background compile future of the raw catalog,
 * consumed on load and discarded on reset.
 */
final class QuestCatalogBoot {

	/** Raw catalog compiled in parallel with other startup work; cleared once consumed. */
	private Future<QuestCatalog> preload;
	/** 预装载后台编译工作线程 / Dedicated background worker thread executing the preload compilation */
	private Thread preloadThread;

	/**
	 * 安排一次后台编译；已安排时忽略重复触发。
	 * Arms one background compilation; repeat calls while armed are ignored.
	 */
	synchronized void preload(Supplier<QuestCatalog> catalogSupplier) {
		if (preload != null) {
			return;
		}
		CompletableFuture<QuestCatalog> future = new CompletableFuture<>();
		preload = future;
		// 使用专用守护线程而非 commonPool：预加载与静态数据阶段并行，commonPool 调度是
		// 启动期静默卡住的疑点之一；独立线程不占静态数据加载池，也不依赖 commonPool。
		// Uses a dedicated daemon thread instead of the commonPool: the preload overlaps the
		// static-data phase and commonPool scheduling was a suspect in silent startup stalls;
		// a dedicated thread neither occupies the static-data pool nor depends on commonPool.
		Thread thread = new Thread(() -> {
			try {
				future.complete(catalogSupplier.get());
			} catch (Throwable t) {
				future.completeExceptionally(t);
			} finally {
				synchronized (QuestCatalogBoot.this) {
					if (preloadThread == Thread.currentThread()) {
						preloadThread = null;
					}
				}
			}
		}, "quest-catalog-preload");
		preloadThread = thread;
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * 是否已安排后台预编译。
	 * Whether a background preload has been armed.
	 *
	 * @return 已安排时为 {@code true} / {@code true} when armed
	 */
	synchronized boolean isArmed() {
		return preload != null;
	}

	/**
	 * 等待预编译结果；未安排预装载时在持锁状态下走调用方给定的同步编译回退。
	 * Await the precompiled result; when preload was not armed, run the caller's synchronous fallback
	 * while still holding the lock (matching the former engine semantics).
	 */
	synchronized QuestCatalog awaitPreload(Supplier<QuestCatalog> syncFallback) throws Exception {
		Future<QuestCatalog> future = preload;
		preload = null;
		preloadThread = null;
		if (future == null) {
			return syncFallback.get();
		}
		try {
			return future.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception exception) {
				throw exception;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw e;
		}
	}

	/**
	 * 丢弃未消费的预装载结果并取消后台任务。
	 * Discard an unconsumed preload result and cancel the background task.
	 */
	synchronized void reset() {
		if (preload != null) {
			preload.cancel(true);
			preload = null;
		}
		if (preloadThread != null) {
			preloadThread.interrupt();
			preloadThread = null;
		}
	}
}
