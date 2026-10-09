package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

import java.util.Objects;

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

	private record OverlayView(QuestCatalog xml, QuestCatalog overlay) {
	}

	private ProductionQuestDefinitions() {
	}

	/** 生产视图目录（惰性编译 + 缓存）。 / The production-view catalog, lazily compiled and cached. */
	public static QuestCatalog catalog() {
		QuestCatalog local = catalog;
		if (local == null) {
			synchronized (ProductionQuestDefinitions.class) {
				local = catalog;
				if (local == null) {
					try {
					local = RetailQuestDriver.overlayProduction(QuestDefinitionDirectoryLoader.compile(
							ProductionQuestDefinitions.class.getClassLoader()));
					} catch (Exception e) {
						throw new IllegalStateException("production quest view unreadable", e);
					}
					catalog = local;
				}
			}
		}
		return local;
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
