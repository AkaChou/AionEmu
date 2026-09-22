package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批次 12 门禁：领奖 NPC 归属核实族（19064 / 29064 / 21455 / 30614）的归属与领奖行合同。
 * 客户端 quest_summary 的行内 NPC 是归属的验收口径：行末写明的 NPC 必须是实现侧的领奖/completion NPC，
 * 起始 NPC 不得再兼任领奖；30614 额外锁定“领奖态入口页必须位于 npc-complete 之后且只出现一次”，
 * 以及 Astella(800327) 与 Aluna(800326) 的归属边界（2026-09-13 的批量对齐曾依据单一 legacy contract
 * 把 30614 错改成 800326）。
 * Locks the twelfth batch: reward-NPC ownership for 19064 / 29064 / 21455 / 30614. The NPC named by the
 * client quest_summary row is the acceptance authority, the start NPC must not double as the reward owner,
 * and 30614 additionally pins the reward entry page after the npc-complete block plus the
 * Astella(800327) versus Aluna(800326) boundary that the 2026-09-13 bulk alignment got wrong.
 */
class RewardNpcOwnershipContractTest {

	/**
	 * 一条归属合同：{@code journalNpc} 是客户端行内写明的 NPC，{@code retiredRewardNpcs} 是不得再出现在
	 * reward -> complete 路线上的旧归属。
	 * One ownership contract: the client-named NPC plus the reward owners that must be gone.
	 */
	private record Ownership(int questId, int journalNpc, Set<Integer> talkNpcIds,
			Set<Integer> retiredRewardNpcs, String rowEvidence) {
	}

	private static final Ownership TEMPLAR_OF_CONSTRUCTION = new Ownership(19064, 203752,
		Set.of(203701, 798450, 203752), Set.of(203701),
		"quest_q19064.html 行 1「带着米拉詹特圣骑士徽章和 relic_02，和 Jucleas 对话」");
	private static final Ownership FANG_OF_CONSTRUCTION = new Ownership(29064, 204075,
		Set.of(204053, 798452, 204075), Set.of(204053),
		"quest_q29064.html 行 1「带着潘利尔的犬牙徽章和 relic_d_02，和 Balder 对话」");
	private static final Ownership INGREDIENTS_FOR_THE_ANTIDOTE = new Ownership(21455, 799244,
		Set.of(799404, 799240, 799244), Set.of(799404, 799240),
		"QUEST_Q21455.html 行 1「把奥德解毒剂交给 Unset」");
	private static final Ownership WRATH_OF_TERATH = new Ownership(30614, 800327,
		Set.of(800327), Set.of(800326),
		"quest_q30614.html 行 1「向 Astella 报告」");

	private static final List<Ownership> OWNERSHIPS = List.of(TEMPLAR_OF_CONSTRUCTION,
		FANG_OF_CONSTRUCTION, INGREDIENTS_FOR_THE_ANTIDOTE, WRATH_OF_TERATH);

	/* 同族参照：客户端行内命名 NPC 与实现侧 owner 早已一致，用于锁定命名约定本身。 */
	/* Family references whose client-named NPC already matched the implemented owner. */
	private static final Map<Integer, Integer> DREADGION_FAMILY = Map.of(
		30610, 800327,
		30611, 800326,
		30612, 800326,
		30613, 800327);

	@Test
	void rewardCompletionBelongsToTheClientJournalNpc() throws Exception {
		for (Ownership ownership : OWNERSHIPS) {
			QuestDefinition definition = definition(ownership.questId());
			Set<Integer> owners = rewardOwners(definition);
			assertTrue(owners.contains(ownership.journalNpc()),
				() -> "quest " + ownership.questId() + " reward owner must be the client-named NPC "
					+ ownership.journalNpc() + " (" + ownership.rowEvidence() + "), owners=" + owners);
			for (int retired : ownership.retiredRewardNpcs()) {
				assertFalse(owners.contains(retired),
					() -> "quest " + ownership.questId() + " retired reward owner " + retired
						+ " still completes the quest");
			}
			assertTrue(talkNpcIds(definition).contains(ownership.journalNpc()),
				() -> "quest " + ownership.questId() + " client-named NPC must stay reachable");
		}
	}

	@Test
	void talkRoutesOnlyAddressTheOwnershipNpcSet() throws Exception {
		for (Ownership ownership : OWNERSHIPS) {
			QuestDefinition definition = definition(ownership.questId());
			assertEquals(ownership.talkNpcIds(), talkNpcIds(definition),
				() -> "quest " + ownership.questId() + " talk route NPC set");
		}
	}

	@Test
	void dreadgionRepeatsFollowTheClientNamingConvention() throws Exception {
		for (Map.Entry<Integer, Integer> sibling : DREADGION_FAMILY.entrySet()) {
			Set<Integer> owners = rewardOwners(definition(sibling.getKey()));
			assertTrue(owners.contains(sibling.getValue()),
				() -> "quest " + sibling.getKey() + " must complete at the client-named NPC "
					+ sibling.getValue() + ", owners=" + owners);
		}
		/* 30614 是族内唯一被 legacy 单源覆盖的任务：owner 与 talk 路线都必须只落在 Astella(800327)。 */
		/* 30614 is the only member the single-source legacy contract overrode: every route must be 800327. */
		QuestDefinition wrath = definition(30614);
		assertEquals(Set.of(800327), rewardOwners(wrath), "30614 reward owner");
		assertEquals(Set.of(800327), talkNpcIds(wrath), "30614 must not keep Aluna(800326) routes");
	}

	@Test
	void staleRewardRowsAreRepairedOnEnterWorld() throws Exception {
		for (int staleRow : List.of(0, 2)) {
			assertRecovery(19064, staleRow);
			assertRecovery(29064, staleRow);
		}
		assertRecovery(21455, 0);
		assertRecovery(30614, 0);
		assertEquals(1, enterWorldRecoveries(definition(21455)).size(), "21455 recovery route count");
		assertEquals(1, enterWorldRecoveries(definition(30614)).size(), "30614 recovery route count");
	}

	@Test
	void rewardEntryPageStaysOutsideTheCompletionBlock() throws Exception {
		String text = resourceText(30614);
		int entryPage = text.indexOf("领奖态入口页");
		assertTrue(entryPage >= 0, "30614 reward entry page");
		assertEquals(-1, text.indexOf("领奖态入口页", entryPage + 1),
			"30614 reward entry page must appear once");
		int completionEnd = text.indexOf("</npc-complete>");
		assertTrue(completionEnd >= 0, "30614 npc-complete block");
		assertTrue(entryPage > completionEnd,
			"30614 reward entry page must follow npc-complete");
		assertTrue(text.contains("<!-- 领奖态入口页：reward + 800327 QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002) -->"),
			"30614 reward entry page must stay on Astella(800327)");
		assertFalse(text.contains("npc-id=\"800326\""), "30614 must not keep Aluna(800326) routes");
	}

	@Test
	void antidoteQuestKeepsOnlyTheClientRoutedButtons() throws Exception {
		String text = resourceText(21455);
		assertFalse(text.contains("action=\"SETPRO2\""), "21455 has no client button for SETPRO2");
		assertTrue(text.contains("<npc-complete npc-id=\"799244\""), "21455 completion owner");
		assertTrue(text.contains("action=\"SETPRO1\"/>"), "21455 item swap stays on Schiemann(799240)");
		assertFalse(text.contains("npc-id=\"799404\" action=\"SELECT_QUEST_REWARD\""),
			"21455 report must not stay on the start NPC Miener(799404)");
		assertFalse(text.contains("npc-id=\"799404\" action=\"QUEST_SELECT\""),
			"21455 stale SELECT5 route must be gone");
	}

	private static void assertRecovery(int questId, int staleRow) throws Exception {
		CompiledQuestDefinition compiled = definitionResource(questId);
		List<QuestTransition> matches = enterWorldRecoveries(compiled.definition()).stream()
			.filter(route -> route.conditions().equals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", staleRow))))
			.toList();
		assertEquals(1, matches.size(),
			() -> "quest " + questId + " recovery route for stale row " + staleRow);
		QuestTransition route = matches.getFirst();
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), route.actions(),
			() -> "quest " + questId + " recovery actions for stale row " + staleRow);
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), route.afterCommit(),
			() -> "quest " + questId + " recovery after-commit for stale row " + staleRow);
		assertNull(route.priority(), () -> "quest " + questId + " recovery priority");

		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, Map.of("var0", staleRow)), route).orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus(), () -> "quest " + questId + " repaired status");
		assertEquals(1, unpack(compiled, plan).get("var0"),
			() -> "quest " + questId + " repaired reward row");
	}

	private static Set<Integer> rewardOwners(QuestDefinition definition) {
		Set<Integer> owners = new LinkedHashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if ("reward".equals(transition.sourceNode()) && "complete".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk) {
				owners.add(talk.npcId());
			}
		}
		return owners;
	}

	private static Set<Integer> talkNpcIds(QuestDefinition definition) {
		Set<Integer> npcIds = new LinkedHashSet<>();
		for (QuestTransition transition : definition.transitions()) {
			if (transition.event() instanceof QuestEvent.TalkToNpc talk) {
				npcIds.add(talk.npcId());
			}
		}
		return npcIds;
	}

	private static List<QuestTransition> enterWorldRecoveries(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(packedVariables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static QuestDefinition definition(int questId) throws IOException {
		return definitionResource(questId).definition();
	}

	private static CompiledQuestDefinition definitionResource(int questId) throws IOException {
		try (InputStream input = RewardNpcOwnershipContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static String resourceText(int questId) throws IOException {
		try (InputStream input = RewardNpcOwnershipContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

}
