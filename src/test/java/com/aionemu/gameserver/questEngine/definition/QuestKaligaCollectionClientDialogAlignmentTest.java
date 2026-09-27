package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证卡里加 20 个收藏品任务的阵营陈列柜和客户端对话合同。
 * Verifies the faction-scoped cabinet and client dialog contract for all 20 Kaliga collection quests.
 */
class QuestKaligaCollectionClientDialogAlignmentTest {
	private static final int KALIGA_KEY_ID = 185000102;
	private static final int KALIGA_BOSS_ID = 217006;
	private static final List<QuestCase> CASES = List.of(
		new QuestCase(18618, 730326, Race.ELYOS),
		new QuestCase(18619, 730327, Race.ELYOS),
		new QuestCase(18620, 730328, Race.ELYOS),
		new QuestCase(18621, 730329, Race.ELYOS),
		new QuestCase(18622, 730330, Race.ELYOS),
		new QuestCase(18623, 730331, Race.ELYOS),
		new QuestCase(18624, 730332, Race.ELYOS),
		new QuestCase(18625, 730333, Race.ELYOS),
		new QuestCase(18626, 730334, Race.ELYOS),
		new QuestCase(18627, 730335, Race.ELYOS),
		new QuestCase(28618, 730326, Race.ASMODIANS),
		new QuestCase(28619, 730327, Race.ASMODIANS),
		new QuestCase(28620, 730328, Race.ASMODIANS),
		new QuestCase(28621, 730329, Race.ASMODIANS),
		new QuestCase(28622, 730330, Race.ASMODIANS),
		new QuestCase(28623, 730331, Race.ASMODIANS),
		new QuestCase(28624, 730332, Race.ASMODIANS),
		new QuestCase(28625, 730333, Race.ASMODIANS),
		new QuestCase(28626, 730334, Race.ASMODIANS),
		new QuestCase(28627, 730335, Race.ASMODIANS));

	@Test
	void allFactionsUseTheirCabinetAndClientPages() throws Exception {
		for (QuestCase questCase : CASES) {
			QuestDefinition definition = load(questCase.questId()).definition();
			assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));
			assertEquals(Set.of(questCase.race().name()), definition.metadata().permittedRaces());

			// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）——接取走 canonicalAcceptFlow
			// （QUEST_SELECT 直发接取窗页 4，无 1007 中转与页梯），交付走 canonicalDelivery
			// （QUEST_SELECT(started→reward) 带钥匙整组门直翻领奖并下发单档奖励窗 1）；START 自环的
			// SELECT5 报告入口页、39/20002 检查对与 SELECT6 失败页整体退场，未集齐零路由。
			// P0-3 S1: the SimpleTalk accept/delivery segments take the retail canonical shape (page 4 /
			// tiered window) — the accept is canonicalAcceptFlow (QUEST_SELECT opens the ask window, page 4)
			// and the delivery is canonicalDelivery (a gated QUEST_SELECT(started->reward) carrying the
			// whole key group, showing the single-tier reward window 1); the START self-loop SELECT5 report
			// entry, the 39/20002 check pair and the SELECT6 failure page are gone.
			QuestTransition accept = route(definition, "unaccepted", "unaccepted",
				new QuestEvent.TalkToNpc(questCase.cabinetId(), QuestDialogAction.QUEST_SELECT.id()), null);
			// 真端形状无路由级种族条件（P0c-13 复験裁定：真端对、XML 错）：阵营隔离由独立任务 ID +
			// race_permitted 元数据在接取时强制（QuestService 全部接取入口校验 isRacePermitted），
			// 状态机入口路由不再重复设防。
			// Retail shape declares no route-level race condition (P0c-13 re-adjudication: retail is
			// right, XML was redundant): faction isolation comes from separate quest ids plus
			// race_permitted metadata enforced at start time (every QuestService start entry checks
			// isRacePermitted), so the entry route stays unguarded.
			assertEquals(List.of(), accept.conditions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), accept.afterCommit());

			QuestTransition deliver = route(definition, "started", "reward",
				new QuestEvent.TalkToNpc(questCase.cabinetId(), QuestDialogAction.QUEST_SELECT.id()), null);
			assertNull(deliver.priority(), "quest " + questCase.questId() + " delivery route priority");
			assertEquals(List.of(new QuestCondition.HasItem(KALIGA_KEY_ID, 1)), deliver.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(KALIGA_KEY_ID, 1)), deliver.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				deliver.afterCommit());

			assertFalse(definition.transitions().stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
						|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())),
				"quest " + questCase.questId() + " retired the 39/20002 check pair");
			assertFalse(definition.transitions().stream().anyMatch(transition ->
				transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.ShowQuestDialog page
						&& (page.dialogId() == QuestDialogPage.SELECT5.id()
							|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
				"quest " + questCase.questId() + " retired the SELECT5/SELECT6 report pages");
			assertFalse(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& transition.event().equals(new QuestEvent.TalkToNpc(KALIGA_BOSS_ID,
						QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()))),
				"quest " + questCase.questId() + " must not check the key at Kaliga");
		}
	}


	private static QuestTransition route(QuestDefinition definition, String source, String target,
		QuestEvent event, Integer priority) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.filter(candidate -> priority == null || priority.equals(candidate.priority()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}

	private record QuestCase(int questId, int cabinetId, Race race) {
	}
}
