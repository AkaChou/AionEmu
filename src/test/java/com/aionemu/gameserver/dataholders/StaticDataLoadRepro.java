package com.aionemu.gameserver.dataholders;

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * 静态数据加载复现 harness（非服务器进程）：复刻 DataManager 启动加载序列，
 * 用于诊断主线程反序列化挂起；看门狗周期性转储全部线程栈。
 * Static-data load reproduction harness (not a server): replays the DataManager
 * startup load sequence to diagnose the main-thread unmarshal hang; a watchdog
 * periodically dumps all thread stacks.
 */
public final class StaticDataLoadRepro {

	public static void main(String[] args) throws Exception {
		System.setProperty("aion.game.data.dir", "src/main/resources/aion/data");
		System.setProperty("aion.game.definitions.dir", "src/main/resources/aion/definitions");
		System.setProperty("aion.game.geo.dir", "src/main/resources/aion/geo");

		if (args.length > 0 && "server".equals(args[0])) {
			serverLike();
			return;
		}

		startWatchdog(45_000, 90_000);

		long start = System.currentTimeMillis();
		DataManager.LoadedStaticData loaded = DataManager.loadStaticData(XmlDataLoader.getInstance());
		System.out.println("REPRO-COMPLETE static=" + (loaded.staticData() != null)
			+ " items=" + loaded.itemData().size()
			+ " npcs=" + loaded.staticData().npcData.size()
			+ " skills=" + loaded.staticData().skillData.size()
			+ " elapsed=" + (System.currentTimeMillis() - start) + "ms");
		Runtime.getRuntime().halt(0);
	}

	/**
	 * 还原 GameStartupSequenceLifecycle 的真实时序：先把 quest 目录编译提交到 commonPool，
	 * 同时在主线程跑完整 DataManager 构造器（含末尾静态字段赋值的 runAsync+get）。
	 * Replays the real GameStartupSequenceLifecycle order: submit the quest catalog compile to commonPool
	 * (equivalent to enginesLifecycle.preloadProductionCatalog), while the full DataManager constructor
	 * runs on the main thread (including the trailing runAsync+get field assignment).
	 */
	private static void serverLike() {
		startWatchdog(60_000, 120_000, 180_000);
		java.util.concurrent.atomic.AtomicReference<Throwable> questFailure = new java.util.concurrent.atomic.AtomicReference<>();
		java.util.concurrent.CompletableFuture<Void> questPreload = java.util.concurrent.CompletableFuture.runAsync(
			() -> com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest.compile(
				com.aionemu.gameserver.configs.Config.dataFile("./data/static_data/quest_definition").toPath()))
			.whenComplete((v, t) -> questFailure.set(t));
		long start = System.currentTimeMillis();
		new DataManager();
		System.out.println("REPRO-SERVER-COMPLETE ctor=" + (System.currentTimeMillis() - start)
			+ "ms questPreloadDone=" + questPreload.isDone() + " questFailure=" + questFailure.get());
		Runtime.getRuntime().halt(0);
	}

	private static void startWatchdog(long... dumpAtMillis) {
		long startedAt = System.currentTimeMillis();
		ThreadMXBean threads = ManagementFactory.getThreadMXBean();
		CountDownLatch done = new CountDownLatch(1);
		Runtime.getRuntime().addShutdownHook(new Thread(done::countDown));
		Thread watchdog = new Thread(() -> {
			try {
				for (long at : dumpAtMillis) {
					long sleep = startedAt + at - System.currentTimeMillis();
					if (sleep > 0 && !done.await(sleep, java.util.concurrent.TimeUnit.MILLISECONDS)) {
						dump(threads, at);
					}
				}
				if (!done.await(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
					System.err.println("REPRO-HUNG: load did not finish; halting for stack capture");
					dump(threads, -1);
					Runtime.getRuntime().halt(2);
				}
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}, "repro-watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}

	private static void dump(ThreadMXBean threads, long at) {
		System.err.println("===== REPRO STACK DUMP at +" + at + "ms =====");
		for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
			Thread thread = entry.getKey();
			ThreadInfo info = threads.getThreadInfo(thread.getId());
			System.err.println("\"" + thread.getName() + "\" state=" + thread.getState()
				+ (info != null && info.getLockName() != null ? " waiting on " + info.getLockName()
					+ (info.getLockOwnerName() != null ? " held by " + info.getLockOwnerName() : "") : ""));
			for (StackTraceElement frame : entry.getValue()) {
				System.err.println("    at " + frame);
			}
		}
		System.err.println("===== END DUMP =====");
	}

	private StaticDataLoadRepro() {
	}
}
