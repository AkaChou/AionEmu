package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 10031/20031 领奖区的区域任务结束广播合同：只广播给真正拥有 zone-mission-end 路由的后续任务，
 * 且完成方绝不把自己列为目标；广播位置与 after-commit 顺序同时锁定。
 * Locks the 10031/20031 reward-stage zone-mission-end broadcast contract: only follow-ups that really own a
 * zone-mission-end route may be targeted, the completing owner must never target itself, and both the
 * broadcast placement and after-commit ordering stay fixed.
 */
class Quest10031And20031ZoneMissionBroadcastTest {
	private static final int[] ELYOS_FOLLOW_UPS = {10032, 10033, 10034, 10035};
	private static final int[] ASMODIAN_FOLLOW_UPS = {20032, 20033, 20034, 20035};

	@Test
	void elyosMissionBroadcastsOnlyRoutableFollowUps() throws Exception {
		assertBroadcastContract(10031, 798927, ELYOS_FOLLOW_UPS);
	}

	@Test
	void asmodianMissionBroadcastsOnlyRoutableFollowUps() throws Exception {
		assertBroadcastContract(20031, 799225, ASMODIAN_FOLLOW_UPS);
	}

	private static void assertBroadcastContract(int questId, int rewardNpcId, int[] followUps)
			throws Exception {
		QuestDefinition definition = load(questId).definition();
		assertNoSelfTarget(definition, questId, followUps);

		// 动作 1009 预览：先广播后续任务，再下发奖励选择窗口。
		// Action 1009 preview: broadcast follow-ups first, then show the reward selection window.
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(rewardNpcId, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		List<AfterCommitAction> previewAfterCommit = preview.afterCommit();
		assertEquals(2, previewAfterCommit.size(), "reward preview after-commit size");
		assertArrayEquals(followUps, assertInstanceOf(AfterCommitAction.BroadcastZoneMissionEnd.class,
			previewAfterCommit.get(0)).questIds());
		assertEquals(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()), previewAfterCommit.get(1));

		// 所有领奖完成分支必须广播同一组后续任务，顺序位于完成状态同步之后、最终选择页之前。
		// Every completion branch must broadcast the same follow-ups, after completion sync and before the final page.
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode()))
			.filter(transition -> "complete".equals(transition.targetNode()))
			.toList();
		assertTrue(completions.size() >= 3, "expected at least three reward completion branches");
		for (QuestTransition completion : completions) {
			List<AfterCommitAction> afterCommit = completion.afterCommit();
			assertEquals(4, afterCommit.size(), "completion after-commit size");
			assertInstanceOf(AfterCommitAction.RefreshPlayerStats.class, afterCommit.get(0));
			assertEquals(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				afterCommit.get(1));
			assertArrayEquals(followUps, assertInstanceOf(AfterCommitAction.BroadcastZoneMissionEnd.class,
				afterCommit.get(2)).questIds());
			assertEquals(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()),
				afterCommit.get(3));
		}

		// USE_OBJECT 只打开领奖入口页；旧 Handler 在该分支不广播。
		// USE_OBJECT only opens the claim page; the legacy handler does not broadcast on that branch.
		QuestTransition open = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(rewardNpcId, QuestDialogAction.USE_OBJECT.id()));
		assertFalse(open.afterCommit().stream()
			.anyMatch(AfterCommitAction.BroadcastZoneMissionEnd.class::isInstance));

		// 每个广播目标都必须实际拥有 zone-mission-end 路由，否则 dispatchOwners 会按缺路由判定投递失败。
		// Every target must own a zone-mission-end route; otherwise dispatchOwners reports a missing route and fails.
		for (int followUp : followUps) {
			assertTrue(hasZoneMissionEndRoute(load(followUp).definition()),
				"quest " + followUp + " must own a zone-mission-end route");
		}
	}

	private static void assertNoSelfTarget(QuestDefinition definition, int questId, int[] followUps) {
		int broadcasts = 0;
		for (QuestTransition transition : definition.transitions()) {
			for (AfterCommitAction action : transition.afterCommit()) {
				if (action instanceof AfterCommitAction.BroadcastZoneMissionEnd broadcast) {
					broadcasts++;
					assertArrayEquals(followUps, broadcast.questIds(),
						"quest " + questId + " broadcast must target the routable follow-ups only");
				}
			}
		}
		assertTrue(broadcasts >= 4,
			"quest " + questId + " must broadcast on the reward preview and every completion branch");
	}

	private static boolean hasZoneMissionEndRoute(QuestDefinition definition) {
		return definition.transitions().stream()
			.anyMatch(transition -> transition.event() instanceof QuestEvent.ZoneMissionEnd);
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(transition -> Objects.equals(source, transition.sourceNode()))
			.filter(transition -> Objects.equals(target, transition.targetNode()))
			.filter(transition -> transition.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = Quest10031And20031ZoneMissionBroadcastTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input,
				"missing quest definition " + questId + ".xml"));
		}
	}
}
