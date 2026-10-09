package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.Map;

import com.aionemu.gameserver.questEngine.retail.RetailQuestTitleIds;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRewardTitlePrerequisiteAuditTest {
	private static final Map<Integer, Integer> REWARD_TITLE_QUESTS = Map.ofEntries(
		Map.entry(19075, 38),
		Map.entry(11033, 107),
		Map.entry(2434, 66),
		Map.entry(2511, 75),
		Map.entry(29074, 88),
		Map.entry(3922, 38),
		Map.entry(3923, 38),
		Map.entry(3924, 38),
		Map.entry(3925, 38),
		Map.entry(3926, 38),
		Map.entry(3927, 38),
		Map.entry(3928, 38),
		Map.entry(3929, 38),
		Map.entry(4923, 88),
		Map.entry(4924, 88),
		Map.entry(4925, 88),
		Map.entry(4928, 88),
		Map.entry(10521, 306),
		Map.entry(20521, 306));

	@Test
	void noQuestRequiresItsOwnRewardTitle() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		for (CompiledQuestDefinition compiled : catalog.all()) {
			QuestDefinition definition = compiled.definition();
			int titlePrereq = definition.metadata().titleId();
			if (titlePrereq > 0) {
				for (QuestReward reward : definition.metadata().rewards()) {
					if ("TITLE".equalsIgnoreCase(reward.kind())) {
						org.junit.jupiter.api.Assertions.assertNotEquals(titlePrereq, reward.id(),
							() -> "quest " + definition.id() + " requires its own reward title " + titlePrereq);
					}
				}
			}
		}
	}

	@Test
	void rewardTitlesDoNotBecomeStartPrerequisites() throws Exception {
		for (Map.Entry<Integer, Integer> entry : REWARD_TITLE_QUESTS.entrySet()) {
			int questId = entry.getKey();
			int titleId = entry.getValue();
			if (nativeOwned(questId)) {
				// P3 重锚（计划 §8.9）：已切原生车道的行不再进 typed 目录，断言面回到原版 quest.xml 行本身 ——
				// reward_titleN 必须解析为该称号，且起始轴（等级/种族/职业/性别/完成前置）没有称号通道。
				// P3 re-anchor (plan §8.9): switched rows left the typed directory, so the assertions come
				// from the retail quest.xml row: reward_titleN must resolve to the title, and the retail
				// acquisition axis carries no title gate.
				NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElseThrow();
				assertEquals(titleId, RetailQuestTitleIds.idOf(titleSymbol(row)),
					"quest " + questId + " 原版 reward_titleN 必须解析为该称号");
				assertTrue(row.fields().keySet().stream().noneMatch(QuestRewardTitlePrerequisiteAuditTest::isTitleGate),
					"quest " + questId + " 原版起始轴不得声明称号前置: " + row.fields().keySet());
				continue;
			}
			QuestMetadata metadata = load(questId).definition().metadata();

			assertEquals(0, metadata.titleId(), "quest " + questId + " has an unexpected title prerequisite");
			assertTrue(metadata.rewards().contains(new QuestReward("TITLE", titleId, 1)),
				"quest " + questId + " must keep title " + titleId + " as a reward");
		}
	}

	/** 原版行声明的称号奖励符号（{@code reward_titleN}；多值取首个声明）。 /
	 * The title reward symbol declared by the retail row (the first {@code reward_titleN}). */
	private static String titleSymbol(NativeQuestXmlTable.QuestRow row) {
		for (int slot = 1; slot <= 4; slot++) {
			String value = row.text("reward_title" + slot);
			if (!value.isBlank()) {
				return value;
			}
		}
		return "";
	}

	/** 称号前置字段判据：原版起始轴只有 {@code finished_quest_cond*}/{@code bm_restrict_category}。 /
	 * Title-gate detector: the retail acquisition axis has no title field. */
	private static boolean isTitleGate(String tag) {
		return tag.equals("title") || tag.endsWith("_title") || tag.startsWith("title_");
	}

	/** 已切换到原生车道的行（SimpleTalk / SimpleHunt）。 / Rows already switched to the native lane. */
	private static boolean nativeOwned(int questId) {
		return SimpleTalkHandler.instance().routes(questId) || SimpleHuntHandler.instance().routes(questId);
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 原版 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}
}
