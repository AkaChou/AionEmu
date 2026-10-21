package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * 生产任务定义视图：XML 目录 + 原版驱动 overlay。
 * <p>
 * 迁移后的任务不再保留 quest-definition XML（历史版本在 git 里可回溯），因此"该任务现在是什么定义"
 * 只能问生产视图：已退役任务由原版模板表 + quest.xml 元数据合成，其余任务仍来自 XML 目录。
 * 一次编译后缓存整个视图，逐任务断言不再重复编译 6224 个定义。
 * <p>
 * Production quest-definition view: the XML directory plus the retail overlay, compiled once and cached.
 */
public final class ProductionQuestDefinitions {

	private static volatile QuestCatalog catalog;
	private static volatile OverlayView overlayView;
	/**
	 * 启动期预编译结果；发布后由 {@link #catalog()} 与静态数据阶段共同复用。
	 * Precompiled startup result, shared by {@link #catalog()} and the static-data phase.
	 */
	private static volatile Future<QuestCatalog> startupPreload;
	/** 启动期预编译已发布的目录 / catalog published by the startup preload. */
	private static volatile QuestCatalog startupCatalog;

	private record OverlayView(QuestCatalog xml, QuestCatalog overlay) {
	}

	private ProductionQuestDefinitions() {
	}

	/** 生产视图目录（惰性编译 + 缓存）。 / The production-view catalog, lazily compiled and cached. */
	public static QuestCatalog catalog() {
		QuestCatalog published = catalog;
		if (published != null) {
			return published;
		}
		Future<QuestCatalog> future;
		synchronized (ProductionQuestDefinitions.class) {
			published = catalog;
			if (published != null) {
				return published;
			}
			published = startupCatalog;
			if (published != null) {
				catalog = published;
				return published;
			}
			future = startupPreload;
			if (future == null) {
				return compileAndCache(ProductionQuestDefinitions::compileDefaultCatalog);
			}
		}
		return await(future);
	}

	/**
	 * 返回共享的生产目录；已有预编译结果时直接复用，否则用给定提供器编译一次。
	 * Returns the shared production catalog, reusing a precompiled result when present; otherwise
	 * compiles once with the supplied provider.
	 *
	 * @param fallbackSupplier 未预编译时的同步编译入口 / synchronous compiler used when no preload is armed
	 * @return 生产目录 / production catalog
	 */
	public static QuestCatalog catalog(Supplier<QuestCatalog> fallbackSupplier) {
		Objects.requireNonNull(fallbackSupplier, "fallbackSupplier");
		Future<QuestCatalog> future;
		synchronized (ProductionQuestDefinitions.class) {
			QuestCatalog published = startupCatalog;
			if (published != null) {
				return published;
			}
			future = startupPreload;
			if (future == null) {
				return compileAndCache(fallbackSupplier);
			}
		}
		return await(future);
	}

	private static QuestCatalog compileAndCache(Supplier<QuestCatalog> catalogSupplier) {
		QuestCatalog compiled = Objects.requireNonNull(catalogSupplier.get(), "compiled catalog");
		catalog = compiled;
		return compiled;
	}

	private static QuestCatalog await(Future<QuestCatalog> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("quest catalog compilation interrupted", e);
		} catch (ExecutionException e) {
			discardFailedPreload(future);
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("quest catalog compilation failed", cause);
		}
	}

	private static void discardFailedPreload(Future<QuestCatalog> future) {
		synchronized (ProductionQuestDefinitions.class) {
			if (startupPreload == future) {
				startupPreload = null;
			}
		}
	}

	/**
	 * 安排在后台编译生产目录；重复安排会被忽略。结果由 {@link #catalog()} 共享，
	 * 静态数据阶段不会再次扫描或校验同一批任务 XML。
	 * Schedules the production catalog compilation in the background; repeated calls are ignored.
	 * The result is shared through {@link #catalog()}, so the static-data phase does not scan or
	 * validate the same quest XML twice.
	 *
	 * @param catalogSupplier 目录编译器 / catalog compiler
	 */
	public static synchronized void preload(Supplier<QuestCatalog> catalogSupplier) {
		Objects.requireNonNull(catalogSupplier, "catalogSupplier");
		if (startupPreload != null || startupCatalog != null) {
			return;
		}
		startupPreload = armPreload(catalogSupplier);
	}

	/**
	 * 在独立守护线程中编译目录，避免与静态数据加载线程互相等待。
	 * Compiles the catalog on a dedicated daemon thread so it never waits on the static-data loaders.
	 *
	 * @param catalogSupplier 目录编译器 / catalog compiler
	 * @return 后台编译结果 / background compilation result
	 */
	private static Future<QuestCatalog> armPreload(Supplier<QuestCatalog> catalogSupplier) {
		CompletableFuture<QuestCatalog> future = new CompletableFuture<>();
		Thread thread = new Thread(() -> {
			try {
				QuestCatalog compiled = Objects.requireNonNull(catalogSupplier.get(), "compiled catalog");
				startupCatalog = compiled;
				future.complete(compiled);
			} catch (Throwable failure) {
				future.completeExceptionally(failure);
			}
		}, "quest-catalog-preload");
		thread.setDaemon(true);
		thread.start();
		return future;
	}

	/**
	 * 测试与开发默认编译入口：直接扫描 classpath 的 quests 目录。
	 * Default test/development compiler: scans the classpath quests directory directly.
	 *
	 * @return 已编译并校验的目录 / compiled and verified catalog
	 */
	private static QuestCatalog compileDefaultCatalog() {
		try {
			return RetailQuestDriver.overlayProduction(QuestDefinitionDirectoryLoader.compile(
				ProductionQuestDefinitions.class.getClassLoader()));
		} catch (Exception e) {
			throw new IllegalStateException("production quest view unreadable", e);
		}
	}

	/** 生产定义（缺失即失败）。 / The production definition; fails when the quest is unknown. */
	public static CompiledQuestDefinition definition(int questId) {
		return catalog().find(questId)
			.orElseThrow(() -> new IllegalStateException("missing production quest definition " + questId));
	}

	/**
	 * 聚焦回归使用真实驱动 overlay，并逐任务校验退役 owner；完整目录覆盖仍由 {@link #catalog()} 校验。
	 * Focused regressions use the real overlay with per-quest retirement checks; catalog() keeps the full gate.
	 */
	public static CompiledQuestDefinition definitionInOverlay(int questId) {
		OverlayView view = overlayView;
		if (view == null) {
			synchronized (ProductionQuestDefinitions.class) {
				view = overlayView;
				if (view == null) {
					try {
						QuestCatalog xml = QuestDefinitionDirectoryLoader.compile(
							ProductionQuestDefinitions.class.getClassLoader());
						view = new OverlayView(xml, RetailQuestDriver.overlay(xml));
					} catch (Exception e) {
						throw new IllegalStateException("retail overlay view unreadable", e);
					}
					overlayView = view;
				}
			}
		}
		if (RetiredQuestIds.contains(questId) && view.xml().find(questId).isPresent()) {
			throw new IllegalStateException("retired quest still has XML owner " + questId);
		}
		return view.overlay().find(questId)
			.orElseThrow(() -> new IllegalStateException("missing retail overlay quest " + questId));
	}

	/** 生产目录条目（含元数据）。 / The production catalog entry. */
	public static QuestCatalogEntry entry(int questId) {
		return Objects.requireNonNull(catalog().findEntry(questId).orElse(null),
			"missing production quest entry " + questId);
	}
}
