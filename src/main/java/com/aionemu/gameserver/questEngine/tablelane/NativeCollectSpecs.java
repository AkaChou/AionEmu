package com.aionemu.gameserver.questEngine.tablelane;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;

/**
 * SimpleCollectItem 相机所需计数的数据来源（计划 §6.2 / P4 切换批）。
 * <p>
 * 采集族的相机 required 就是真端 {@code quest.xml} 的 {@code collect_item1..N} 计数
 * （与交付门的 HasItem 判据同源，禁止第二套事实）。本类只做「真端列 → 单槽 required 映射」的
 * 纯转换：{@code collect_itemK} 的第 K 项即槽 K（253/262 行只有一项，即槽 1）。
 * 缺行/物品符号未解析的行返回空映射，调用方（{@link CameraRegistry}）不为该行派生相机行，
 * 处理器随后将其视为不可路由——fail-closed，不伪造计数。
 * <p>
 * Camera-requirement source for the SimpleCollectItem family. The collect camera requirement is
 * exactly the retail {@code quest.xml} {@code collect_item1..N} count (the same source as the
 * hand-in HasItem gate; never a second source of truth). This class only maps retail columns onto a
 * single-slot requirement map: the Kth {@code collect_itemK} entry is slot K (253 of 262 rows carry
 * one entry, i.e. slot 1). Missing rows and unresolved item symbols yield an empty map; the caller
 * ({@link CameraRegistry}) then derives no camera row and the handler treats the row as unroutable,
 * which fails closed instead of inventing a count.
 */
final class NativeCollectSpecs {

	private NativeCollectSpecs() {
	}

	/** 采集行 → 槽 required 映射（槽 1..N，缺行/未解即空）。 / Collect row → slot requirement map (empty when missing or unresolved). */
	static Map<Integer, Integer> collectSlotRequirements(int questId) {
		return collectSlotRequirements(questId, null);
	}

	/**
	 * 相机所需计数；元数据不可编译的行（如真端 {@code minlevel_permitted=999} 的休眠行
	 * 36017/46017/47112，min&gt;max 会被 {@code QuestMetadata} 拒绝）记入 {@code unresolved} 并返回空映射。
	 * Requirements; rows whose metadata cannot compile (the retail dormant rows with
	 * {@code minlevel_permitted=999} — 36017/46017/47112 — are rejected by {@code QuestMetadata}
	 * because min &gt; max) are recorded in {@code unresolved} and yield an empty map.
	 */
	static Map<Integer, Integer> collectSlotRequirements(int questId, Set<Integer> unresolved) {
		RetailQuestMetadataCompiler.Outcome outcome;
		try {
			outcome = RetailQuestDriver.ensureLoaded().retailMetadataOf(questId).orElse(null);
		} catch (java.io.IOException | RuntimeException e) {
			if (unresolved != null) {
				unresolved.add(questId);
			}
			return Map.of();
		}
		if (outcome == null) {
			return Map.of();
		}
		List<QuestItemRequirement> collected = outcome.metadata().itemRequirements();
		if (collected.isEmpty()) {
			return Map.of();
		}
		Map<Integer, Integer> requirements = new LinkedHashMap<>(collected.size());
		for (int index = 0; index < collected.size(); index++) {
			requirements.put(index + 1, collected.get(index).count());
		}
		return requirements;
	}
}
