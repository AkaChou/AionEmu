package com.aionemu.gameserver.questEngine.tablelane;

import java.util.Map;

/**
 * 原版 NPC 势力名 → 势力 id 表（两条车道共用的唯一事实来源）。
 * <p>
 * 来源：服务端静态数据的全库投票（同一势力名只对应一个 id，零歧义，P0a 复核）；原版 {@code quest.xml}
 * 的 {@code npcfaction_name} 用它换算 {@code npcFactionId}，家族表的 {@code <地图>_<势力名>} 复合交付名
 * 也用它识别。旧 retail 元数据编译器在 P8 删除前同样引用本表，避免出现第二个名→id 常量表。
 * Retail NPC-faction name to id, the single fact source shared by both lanes (library-wide unambiguous
 * votes, re-checked in P0a). The retail metadata compiler references this table too until it retires.
 */
public final class NativeNpcFactionNames {

	private static final Map<String, Integer> BY_NAME = Map.ofEntries(
		Map.entry("Army_Da", 6), Map.entry("Army_Li", 3), Map.entry("BountyHunter_Da", 7),
		Map.entry("BountyHunter_Li", 4), Map.entry("Greenhat_D", 18), Map.entry("Greenhat_L", 17),
		Map.entry("GuardianOfDivine", 2), Map.entry("GuardianOfTower", 5), Map.entry("Silverlin_D", 16),
		Map.entry("Silverlin_L", 15));

	private NativeNpcFactionNames() {
	}

	/** 势力名 → id；未知势力名返回 0（客户端不可见势力）。 / Faction name to id; 0 when unknown. */
	public static int idOf(String name) {
		if (name == null) {
			return 0;
		}
		return BY_NAME.getOrDefault(name.trim(), 0);
	}

	/** 全部已知势力名 → id（只读）。 / All known faction names to ids (read-only). */
	public static Map<String, Integer> all() {
		return BY_NAME;
	}
}
