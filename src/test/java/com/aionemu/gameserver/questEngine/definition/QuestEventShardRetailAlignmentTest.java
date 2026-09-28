package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural coverage for the six event shard owners 50031/50038/50040/50041/50073/50074. */
class QuestEventShardRetailAlignmentTest {


	/**
	 * 已退役、由真端模板合成的两支：结构 pin 断真端族形（网格阶梯）而不是遗留 XML 的形，且计数轴以
	 * 客户端进度行（{@code quest_client_hunt_progress_rows.tsv}）为准，不再照抄遗留 XML 的 var1 上限。
	 * The two retired, retail-synthesized rows: the pin asserts the retail family shape (kill grid ladder)
	 * instead of the legacy XML shape, and the count axis comes from the client progress row rather than
	 * the legacy XML's var1 ceiling.
	 */
	private static final Set<Integer> RETAIL_OWNED = Set.of(50073, 50074);

	/**
	 * 真端阶梯的击杀目标：DD 行写出的 {@code IDEvent_Solo_Saam_65_N}（246293）连同其客户端显示名同族模板
	 * （{@code name_id=2306478} 的 246326，45 级变体）——真端 hunt 族按显示名族展开，客户端看到的是同一个名字。
	 * Retail ladder kill targets: the name written by the DD row ({@code IDEvent_Solo_Saam_65_N} → 246293)
	 * plus its client display-name family sibling (246326, the level-45 variant sharing {@code name_id=2306478});
	 * the retail hunt family expands by display-name family, which is the name the client shows.
	 */
	private static final Map<Integer, Set<Integer>> RETAIL_KILL_TARGETS = Map.of(
		50073, Set.of(246293, 246326), 50074, Set.of(246293, 246326));

	/**
	 * 生产视图装载：50073/50074 已退役、由真端模板合成，其余四支仍在 XML 目录；overlay 同时覆盖两条路径。
	 * Loads through the production view: 50073/50074 are retired and synthesized from the retail tables while the
	 * other four still come from the XML directory — the overlay covers both paths.
	 */
	private static CompiledQuestDefinition load(int questId) {
		return ProductionQuestDefinitions.definitionInOverlay(questId);
	}

	/** Expected metadata per quest: (name, display-name-id, min-level, daily, weekly, cannot-share, race, rewards). */
	private static final Map<Integer, Facts> EXPECTED = Map.of(
		50031, new Facts("[Event/Daily] Deal a critical love hit", 1150198, 21, true, false, true, "ELYOS",
			List.of(new QuestReward("EXP", 0, 77777), new QuestReward("ITEM", 188100117, 10))),
		50038, new Facts("[Event/Daily] Orders From Above", 1800100, 25, true, false, true, "ELYOS",
			List.of(new QuestReward("ITEM", 188100181, 1))),
		50040, new Facts("[Event/Weekly] Death to the Officers", 1800102, 25, false, true, true, "ELYOS",
			List.of(new QuestReward("ITEM", 188053253, 1))),
		50041, new Facts("[Event/Weekly] Death to the Generals", 1800103, 25, false, true, true, "ELYOS",
			List.of(new QuestReward("ITEM", 166030007, 1))),
		50073, new Facts("[Event] Attack on the Kumuki Hideout", 1803481, 46, false, false, true, "PC_ALL",
			List.of(new QuestReward("EXP", 0, 20000000), new QuestReward("ITEM", 162001063, 5))),
		50074, new Facts("[Event] Major attack on the Kumuki Hideout", 1803482, 51, false, false, true, "PC_ALL",
			List.of(new QuestReward("EXP", 0, 55000000), new QuestReward("ITEM", 162001063, 5))));

	/** Hunt steps per quest from retail quest.xml progress_info. */
	private static final Map<Integer, Integer> KILL_STEPS = Map.of(
		50031, 5, 50038, 5, 50040, 20, 50041, 6, 50073, 15, 50074, 15);

	/** Turn-in npcs per quest (report dialog 1009 from the last kill node). */
	private static final Map<Integer, Set<Integer>> REPORT_NPCS = Map.of(
		50031, Set.of(831783), 50038, Set.of(832815), 50040, Set.of(832815),
		50041, Set.of(832815), 50073, Set.of(835570, 835571), 50074, Set.of(835570, 835571));

	@Test
	void metadataMatchesRetailQuestData() throws Exception {
		for (Map.Entry<Integer, Facts> entry : EXPECTED.entrySet()) {
			int questId = entry.getKey();
			Facts expected = entry.getValue();
			CompiledQuestDefinition compiled = load(questId);
			QuestMetadata metadata = compiled.definition().metadata();
			assertEquals(questId, compiled.id(), "id of " + questId);
			if (RETAIL_OWNED.contains(questId)) {
				// 真端合成的 name 是 id 占位串（客户端只读 displayNameId 对应的本地化名字）。
				// The retail-synthesized name is the id placeholder; the client reads the localized name
				// through displayNameId.
				assertEquals("Q" + questId, metadata.name(), "retail name placeholder of " + questId);
			} else {
				assertEquals(expected.name(), metadata.name(), "name of " + questId);
			}
			assertEquals(expected.displayNameId(), metadata.displayNameId(), "display-name-id of " + questId);
			assertEquals(expected.minLevel(), metadata.minLevel(), "min-level of " + questId);
			assertEquals("EVENT", metadata.category(), "category of " + questId);
			assertEquals(expected.cannotShare(), metadata.cannotShare(), "cannot-share of " + questId);
			assertEquals(Set.of(expected.race()), metadata.permittedRaces(), "race of " + questId);
			assertEquals(new RepeatPolicy(255, 0, expected.daily(), expected.weekly()),
				metadata.repeatPolicy(), "repeat policy of " + questId);
			assertEquals(expected.rewards(), metadata.rewards(), "rewards of " + questId);
		}
	}

	@Test
	void huntStepsUseAnyWorldWildcardOrTheRetailKumukiMob() throws Exception {
		for (int questId : new int[] {50031, 50038, 50040, 50041}) {
			List<QuestEvent> kills = load(questId).definition().transitions().stream()
				.map(QuestTransition::event)
				.filter(event -> event instanceof QuestEvent.KillInWorld)
				.toList();
			assertEquals(KILL_STEPS.get(questId), kills.size(), "kill-in-world steps of " + questId);
			for (QuestEvent kill : kills) {
				assertEquals(0, ((QuestEvent.KillInWorld) kill).worldId(),
					"quest " + questId + " kill step must use the any-world wildcard");
			}
		}
		for (int questId : new int[] {50073, 50074}) {
			CompiledQuestDefinition compiled = load(questId);
			// 真端 hunt 族形：阶梯节点 a0..aN 每步一条按 npc 的 KillNpc 边（遗留 XML 写作单个
			// KillNpcSet，运行期路由等价，见 RetailKillRoutes）；阶梯深度即客户端进度行的计数，
			// 因此这里以客户端进度行为准，而不是遗留 XML 的 var1 上限。
			// Retail hunt family shape: every ladder step a0..aN carries one per-npc KillNpc edge (the
			// legacy XML wrote a single KillNpcSet; both route identically, see RetailKillRoutes) and the
			// ladder depth is the client progress-row count, so the client row decides the ceiling.
			int clientCount = clientHuntCount(questId);
			assertEquals(KILL_STEPS.get(questId), clientCount, "client hunt count of " + questId);
			Map<Integer, Integer> killSteps = new TreeMap<>();
			for (QuestTransition transition : compiled.definition().transitions()) {
				if (transition.event() instanceof QuestEvent.KillNpc kill) {
					killSteps.merge(kill.npcId(), 1, Integer::sum);
				}
			}
			assertEquals(RETAIL_KILL_TARGETS.get(questId), killSteps.keySet(), "kill targets of " + questId);
			killSteps.forEach((npcId, steps) -> assertEquals(clientCount, steps,
				"kill steps of npc " + npcId + " in " + questId));
		}
	}

	/** 客户端进度行的计数（每任务恰一行；多行说明该任务不是单段形，须重新裁定）。 */
	private static int clientHuntCount(int questId) {
		var rows = com.aionemu.gameserver.questEngine.retail.RetailClientHuntProgressRows.defaultHuntProgressRows().rows(questId);
		assertEquals(1, rows.size(), "client progress rows of " + questId);
		return rows.getFirst().count();
	}

	@Test
	void reportAndCompletionRoutesMatchRetailTurnIn() throws Exception {
		for (int questId : EXPECTED.keySet()) {
			CompiledQuestDefinition compiled = load(questId);
			List<QuestTransition> transitions = compiled.definition().transitions();

			// 交付 NPC 按"非领奖态进领奖的对话路由"提取，形状二分皆可：规范形 = 满段 QUEST_SELECT
			// 交付，遗留形 = 1009 上交。
			// Delivery npcs via the non-reward dialog route into reward, either shape: the canonical
			// full-node QUEST_SELECT delivery or the legacy 1009 turn-in.
			Set<Integer> reportNpcs = new HashSet<>();
			for (QuestTransition transition : transitions) {
				if (transition.targetNode().equals("reward")
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())
					&& !transition.sourceNode().equals("reward")) {
					reportNpcs.add(talk.npcId());
				}
			}
			assertEquals(REPORT_NPCS.get(questId), reportNpcs, "report npcs of " + questId);

			// 50073/50074 双 NPC 各展开 8..23 的 16 条完成路线，其余单 NPC 16 条；
			// 真端合成的两支还各带一条 108 领奖窗确认完成路线（RetailRewardWindowRouteTest 覆盖的同一词汇）。
			// The retired pair expands 16 reward routes per npc like the rest, plus one dialog-108
			// reward-window confirmation route per npc (the vocabulary RetailRewardWindowRouteTest covers).
			int perNpc = RETAIL_OWNED.contains(questId) ? 17 : 16;
			List<List<QuestAction>> completions = transitions.stream()
				.filter(t -> t.sourceNode().equals("reward"))
				.filter(t -> t.targetNode().equals("complete"))
				.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
				.map(QuestTransition::actions).toList();
			assertEquals(perNpc * REPORT_NPCS.get(questId).size(), completions.size(),
				"completion route count of " + questId);
			for (List<QuestAction> path : completions) {
				for (QuestReward reward : EXPECTED.get(questId).rewards()) {
					if ("EXP".equals(reward.kind())) {
						assertTrue(path.contains(new QuestAction.GrantReward("EXP", 0, reward.amount(),
							QuestRewardAmountMode.QUEST_BASE)), "exp grant of " + questId);
					} else {
						assertTrue(path.contains(new QuestAction.GrantReward(reward.kind(), reward.id(),
							reward.amount())), "reward grant of " + questId);
					}
				}
				assertTrue(path.contains(new QuestAction.CompleteQuest(0)), "completion of " + questId);
			}
		}
	}

	private record Facts(String name, int displayNameId, int minLevel, boolean daily, boolean weekly,
		boolean cannotShare, String race, List<QuestReward> rewards) {
	}
}
