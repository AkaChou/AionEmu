package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;

/**
 * 阵营日常轮换的族间共用判定（计划 §7 P4 收口）。
 * <p>
 * 该判定只读原版 {@code quest.xml} 的轴（等级/种族/职业/性别/重复上限）与玩家势力状态，**与家族无关**；
 * P3 时它随 SimpleTalk 落地，P4 起第二个家族（SimpleCollectItem）需要同一条事实，故抽为共用实现，两个
 * 家族 handler 只提供"本行是否系统发放 / 势力 id"两项输入，避免出现第二套轴判定。
 * <p>
 * Family-independent faction-rotation adjudication, sharing one implementation between the switched
 * families: the axes come from the retail {@code quest.xml} row and the per-row inputs (system-grant
 * verdict, faction id) are supplied by the family handler.
 */
final class NativeFactionRotation {

	private NativeFactionRotation() {
	}

	/** 原版 {@code quest.xml} 行声明的势力 id（{@code npcfaction_name}；无则 0）。 */
	static int factionIdOf(int questId) {
		return NativeNpcFactionNames.idOf(NativeQuestXmlTable.instance().find(questId)
			.map(row -> row.text("npcfaction_name")).orElse(""));
	}

	/**
	 * 阵营日常轮换资格：原版 {@code quest.xml} 轴直读（不构造 IR 元数据）。
	 * Faction-rotation eligibility, read straight from the retail quest.xml axes.
	 */
	static boolean eligible(Player player, int questId, int factionId, boolean systemGranted,
			boolean factionKind, int questFactionId) {
		if (player == null || !systemGranted || !factionKind || factionId <= 0
				|| questFactionId != factionId) {
			return false;
		}
		var factions = player.getNpcFactions();
		var faction = factions == null ? null : factions.getNpcFactionById(factionId);
		if (faction == null || !faction.isActive()) {
			return false;
		}
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElse(null);
		if (row == null || player.getQuestStateList() == null) {
			return false;
		}
		QuestState existing = player.getQuestStateList().getQuestState(questId);
		boolean repeatable = intOrZero(row, "max_repeat_count") > 0;
		if (existing != null && existing.getStatus() != QuestStatus.NONE
				&& existing.getStatus() != QuestStatus.LOCKED && !repeatable) {
			return false;
		}
		// 轴判定与 NPC 接取共用同一实现（NativeQuestStartPort），避免第二套事实来源。
		int minLevel = intOrZero(row, "minlevel_permitted");
		int maxLevel = intOrDefault(row, "maxlevel_permitted", Integer.MAX_VALUE);
		int level = player.getLevel();
		if (minLevel != 999 && level < minLevel) {
			// 系统发放的阵营日常不走 NPC 接取（原版 CanAcquireQuest 不参与），故 999 在此不阻断。
			return false;
		}
		if (level > maxLevel) {
			return false;
		}
		if (!NativeQuestStartPort.racePermitted(row.text("race_permitted"),
				player.getRace() == null ? null : player.getRace().name())) {
			return false;
		}
		String classToken = player.getCommonData() == null || player.getCommonData().getPlayerClass() == null
				? null
				: player.getCommonData().getPlayerClass().name().toUpperCase(java.util.Locale.ROOT);
		// 职业轴与 NPC 接取/生产元数据同源（原版 token → PlayerClass）。
		// The class axis shares the retail token mapping used by NPC acquisition and quest metadata.
		java.util.Set<String> permittedClasses = RetailQuestMetadataCompiler.permittedClassNames(
				row.text("class_permitted"), minLevel);
		if (!permittedClasses.isEmpty() && (classToken == null || !permittedClasses.contains(classToken))) {
			return false;
		}
		String genderToken = player.getGender() == null ? null
				: player.getGender().name().toLowerCase(java.util.Locale.ROOT);
		return NativeQuestStartPort.tokenPermitted(row.text("gender_permitted"), genderToken);
	}

	/** 原版该类别是否为阵营日常哨兵。 / Whether the retail category is the faction sentinel. */
	static boolean factionKind(RetailGrantKind kind) {
		return kind == RetailGrantKind.FACTION;
	}

	private static int intOrZero(NativeQuestXmlTable.QuestRow row, String tag) {
		Integer value = row.integer(tag);
		return value == null ? 0 : value;
	}

	private static int intOrDefault(NativeQuestXmlTable.QuestRow row, String tag, int fallback) {
		Integer value = row.integer(tag);
		return value == null ? fallback : value;
	}
}
