package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Retail-anchored grid coverage for the special mission hunt owner 19636 (definition from the
 * production driver): the metadata pins stay on the retail table, the ten-kill grid advances
 * state by state, unfinished 1009 routes stay behind the var0 gate, and the reward close keeps
 * the thirteen selectable branches.
 */
class Quest19636RetailAlignmentTest {
	private static final int REPORT_NPC = 798155;
	private static final int KILLS_REQUIRED = 10;
	/** 客户端 quest_monster 变体名单（驱动显示名扩展是合法超集）。 / Client quest_monster variants (driver display-name expansion is a legal superset). */
	private static final Set<Integer> CLIENT_TARGETS = Set.of(214263, 214264, 214265, 214266);

	@Test
	void preservesMetadataTenKillsAndThirteenSelectableBranches() {
		QuestDefinition definition = load().definition();
		QuestMetadata metadata = definition.metadata();
		// 真端驱动合成行名 Q19636，客户端锚点保留在 displayNameId。
		// The retail driver synthesizes the row name Q19636; the client anchor stays in displayNameId.
		assertEquals("Q19636", metadata.name());
		assertEquals(1800381, metadata.displayNameId());
		assertEquals(45, metadata.minLevel());
		assertEquals("IMPORTANT", metadata.category());
		assertEquals(Set.of("ELYOS"), metadata.permittedRaces());
		assertEquals(Set.of(19635), metadata.prerequisites());
		assertEquals(new QuestReward("EXP", 0, 6242224), metadata.rewards().get(0));
		assertEquals(13, metadata.rewards().stream()
			.filter(reward -> "SELECTABLE_ITEM".equals(reward.kind())).count());

		List<QuestTransition> transitions = definition.transitions();
		// 计数器合同：真端把 10 杀投影为 a0..a10 网格，逐杀推进（PACKET_ONLY）。
		// Counter contract: the retail shape projects the ten kills as the a0..a10 grid advancing
		// one kill at a time (PACKET_ONLY).
		int required = rewardProjection(definition);
		assertEquals(KILLS_REQUIRED, required, "19636 kill counter ceiling");
		for (int kills = 0; kills < required; kills++) {
			final int state = kills;
			List<QuestTransition> edges = transitions.stream()
				.filter(transition -> Objects.equals(transition.sourceNode(), "a" + state)
					&& transition.event() instanceof QuestEvent.KillNpc)
				.toList();
			assertFalse(edges.isEmpty(), () -> "a" + state + " 必须携带击杀边");
			Set<Integer> targets = new TreeSet<>();
			for (QuestTransition edge : edges) {
				assertEquals("a" + (state + 1), edge.targetNode());
				assertEquals(List.of(), edge.conditions());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					edge.afterCommit());
				targets.add(((QuestEvent.KillNpc) edge.event()).npcId());
			}
			assertTrue(targets.containsAll(CLIENT_TARGETS),
				() -> "a" + state + " 击杀集必须覆盖客户端变体，实际 " + targets);
		}

		// 满格报告：满格 1009 无门禁进领奖，未满格 1009 由 var0=10 门禁。
		// Full-count report: the saturated 1009 enters reward ungated; unfinished 1009 routes are
		// gated on var0=10.
		for (int kills = 0; kills < required; kills++) {
			final int state = kills;
			QuestTransition gated = talkRewardRoute(transitions, state);
			assertEquals("reward", gated.targetNode());
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", KILLS_REQUIRED)),
				gated.conditions(), () -> "a" + state + " 满格恢复必须带门禁");
		}
		QuestTransition finish = talkRewardRoute(transitions, required);
		assertEquals("reward", finish.targetNode());
		assertEquals(List.of(), finish.conditions());

		// 领奖收口：16 条确认路由，其中 13 条发放可选物品（与真端 SELECTABLE_ITEM 行数一致）。
		// Reward close: sixteen confirm routes, thirteen of which grant a selectable item (matching
		// the retail SELECTABLE_ITEM rows).
		List<QuestTransition> completions = transitions.stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.toList();
		assertEquals(16, completions.size());
		assertEquals(13, completions.stream()
			.filter(transition -> transition.actions().stream()
				.anyMatch(action -> action instanceof QuestAction.GrantReward grant
					&& "ITEM".equals(grant.kind())))
			.count());

		// 1009 报告 owner 独占 798155。
		// The 1009 report owner stays exclusive to 798155.
		assertEquals(Set.of(REPORT_NPC), transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == 1009)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}

	private static QuestTransition talkRewardRoute(List<QuestTransition> transitions, int state) {
		return transitions.stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "a" + state))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == REPORT_NPC && Integer.valueOf(1009).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	/** 领奖投影携带的击杀网格规模。 / The kill-grid size carried by the reward projection. */
	private static int rewardProjection(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> "reward".equals(node.label()))
			.findFirst().orElseThrow().projection().variables().get("var0");
	}

	/** 生产驱动定义（已退役的 XML 只在 git 历史）。 / The production-driver definition (the retired XML lives only in git history). */
	private static CompiledQuestDefinition load() {
		return ProductionQuestDefinitions.definition(19636);
	}
}
