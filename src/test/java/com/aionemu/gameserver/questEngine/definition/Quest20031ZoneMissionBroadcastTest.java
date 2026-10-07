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
 * 锁定任务 20031 领奖区的区域任务结束广播合同：只广播给具备 zone-mission-end 路由的后续任务，
 * 且完成方绝不把自己列为目标；广播位置与 after-commit 顺序同时锁定。
 * Locks quest 20031's reward-stage zone-mission-end broadcast contract: only follow-ups that own a
 * zone-mission-end route may be targeted, the completing owner must never target itself, and both the
 * broadcast placement and after-commit ordering stay fixed.
 * <p>
 * 天族孪生 10031 已退役（保留清单 owner=RETAIL_TABLE，XML 只在 git 历史）⇒ 本类只保魔族半；
 * 广播目标中的已退役后续任务（20032/20033/20034）转 native 车道后无 typed 定义，其路由归属由
 * 车道承担，此处只对仍存 XML 的目标核对路由。
 * <p>
 * The Elyos twin 10031 is retired (XML only in git history), so only the Asmodian half remains. The
 * retired follow-ups (20032/20033/20034) moved to the native lanes and have no typed definition, so the
 * route ownership check covers the XML-owned targets only; the broadcast list itself stays a definition fact.
 */
class Quest20031ZoneMissionBroadcastTest {
	private static final int[] ASMODIAN_FOLLOW_UPS = {20032, 20033, 20034, 20035};

	@Test
	void asmodianMissionBroadcastsOnlyRoutableFollowUps() throws Exception {
		QuestDefinition definition = load(20031).definition();
		assertNoSelfTarget(definition);

		// 动作 1009 预览：先广播后续任务，再下发奖励选择窗口。
		// Action 1009 preview: broadcast follow-ups first, then show the reward selection window.
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(799225, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		List<AfterCommitAction> previewAfterCommit = preview.afterCommit();
		assertEquals(2, previewAfterCommit.size(), "reward preview after-commit size");
		assertArrayEquals(ASMODIAN_FOLLOW_UPS, assertInstanceOf(AfterCommitAction.BroadcastZoneMissionEnd.class,
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
			assertArrayEquals(ASMODIAN_FOLLOW_UPS, assertInstanceOf(AfterCommitAction.BroadcastZoneMissionEnd.class,
				afterCommit.get(2)).questIds());
			assertEquals(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()),
				afterCommit.get(3));
		}

		// USE_OBJECT 只打开领奖入口页；旧 Handler 在该分支不广播。
		// USE_OBJECT only opens the claim page; the legacy handler does not broadcast on that branch.
		QuestTransition open = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(799225, QuestDialogAction.USE_OBJECT.id()));
		assertFalse(open.afterCommit().stream()
			.anyMatch(AfterCommitAction.BroadcastZoneMissionEnd.class::isInstance));

		// 仍存 XML 的广播目标必须实际拥有 zone-mission-end 路由；已退役目标（native 车道）跳过路由核对，
		// 但其退役身份必须成立——广播命中由车道面承担。
		// XML-owned targets must own a zone-mission-end route; retired targets skip the route check but
		// their retirement must hold (the lane faces carry the broadcast delivery).
		int checked = 0;
		for (int followUp : ASMODIAN_FOLLOW_UPS) {
			if (RetiredQuestIds.contains(followUp)) {
				continue;
			}
			assertTrue(hasZoneMissionEndRoute(load(followUp).definition()),
				"quest " + followUp + " must own a zone-mission-end route");
			checked++;
		}
		assertTrue(checked >= 1, "至少一个仍存 XML 的广播目标必须接受路由核对");
	}

	private static void assertNoSelfTarget(QuestDefinition definition) {
		int broadcasts = 0;
		for (QuestTransition transition : definition.transitions()) {
			for (AfterCommitAction action : transition.afterCommit()) {
				if (action instanceof AfterCommitAction.BroadcastZoneMissionEnd broadcast) {
					broadcasts++;
					assertArrayEquals(ASMODIAN_FOLLOW_UPS, broadcast.questIds(),
						"quest 20031 broadcast must target the routable follow-ups only");
				}
			}
		}
		assertTrue(broadcasts >= 4,
			"quest 20031 must broadcast on the reward preview and every completion branch");
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
		try (InputStream input = Quest20031ZoneMissionBroadcastTest.class.getResourceAsStream(
			"/aion/data/static_data/quest/definitions/quests/" + questId + ".xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input,
				"missing quest definition " + questId + ".xml"));
		}
	}
}
