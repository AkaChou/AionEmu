package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 24：客户端 quest_summary 的“空槽位”边界，防止 QE-051 的行号口径被机械套用到没有任务书行的任务上。
 * <p>
 * 客户端 `QUEST_Q1000.html` / `QUEST_Q2000.html`（序幕 / Prologue）的 quest_summary 固定渲染 4 个 `<step>`，
 * 但每一行的可见文本都是空白（行 0 只挂 `[%collectitem]` 占位符，而 quest.xml 里 1000/2000 既没有 collect_item
 * 也没有任何 NPC），服务端对应实现是“进入新手区 → 播放序章影片 → 影片结束即完成”，**没有 REWARD 节点、没有领奖行**。
 * 行号口径的审计会把这类模板判成 NO_REWARD_ROW / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE，按“补回行 1/2/3”去改
 * 只会造出永远不显示的任务书节点。
 * <p>
 * 同族边界（只登记、不在本批改定义）：`1400`（Paion's Worry）的行 1 是空槽，行 0 是“除掉作恶的特洛尔和托尔金 (/7)”，
 * `var0`/`var1` 是 8×4 的击杀计数组合（35 个节点）而不是行号，reward 投影 `var0=7,var1=3` 是计数饱和值；
 * 另有 10 个客户端有任务书但服务端暂无节点/定义的 id 登记在审计脚本的 BLANK_JOURNAL_SLOT_EXCEPTIONS。
 * <p>
 * Locks batch 24: the blank quest_summary slots of the prologue quests (1000/2000) and the counter-slot boundary of
 * 1400. The prologue template renders four empty rows and the Elyos/Asmodian prologue completes through the intro
 * movie with no REWARD node, so the QE-051 row index lens must not grow journal rows here; 1400 keeps its 8x4 kill
 * counter combination and its saturated reward projection.
 */
class BlankJournalSlotBoundaryContractTest {

	private static final int ELYOS_PROLOGUE = 1000;
	private static final int ASMODIAN_PROLOGUE = 2000;
	private static final int KILL_COUNTER = 1400;
	private static final int PAION_OWNER = 203941;

	@Test
	void prologueQuestsCompleteWithoutAnyRewardRow() throws Exception {
		for (int questId : List.of(ELYOS_PROLOGUE, ASMODIAN_PROLOGUE)) {
			QuestDefinition definition = definition(questId).definition();
			assertEquals(Set.of("unaccepted", "started", "complete"),
				definition.nodes().stream().map(QuestNode::label).collect(Collectors.toSet()),
				() -> "quest " + questId + " prologue node shape");
			assertEquals(QuestStatus.COMPLETE, node(definition, "complete").projection().status());
			assertTrue(definition.nodes().stream()
					.noneMatch(node -> node.projection().status() == QuestStatus.REWARD),
				() -> "quest " + questId + " is a prologue and owns no reward row");
			assertTrue(definition.transitions().stream()
					.noneMatch(route -> "reward".equals(route.targetNode())),
				() -> "quest " + questId + " must not grow a reward route");
		}
	}

	@Test
	void prologueQuestsKeepTheirIntroMovieAndEnterZoneTriggers() throws Exception {
		assertPrologue(ELYOS_PROLOGUE, "AKARIOS_PLAINS_210010000", 1);
		assertPrologue(ASMODIAN_PROLOGUE, "ALDELLE_BASIN_220010000", 2);
	}

	@Test
	void prologueQuestsMustNotGrowJournalRows() throws Exception {
		for (int questId : List.of(ELYOS_PROLOGUE, ASMODIAN_PROLOGUE)) {
			QuestDefinition definition = definition(questId).definition();
			/* 序幕没有 NPC 对话，也没有 s1/s2/s3 之类的行节点——这两样出现即说明按空槽补了阶梯。 */
			/* The prologue has no NPC dialog and no s1/s2/s3 row nodes; either would mean rows were invented. */
			assertTrue(definition.transitions().stream()
					.noneMatch(route -> route.event() instanceof QuestEvent.TalkToNpc),
				() -> "quest " + questId + " prologue must stay dialog-free");
			assertTrue(definition.nodes().stream().noneMatch(node -> node.label().matches("s\\d+")),
				() -> "quest " + questId + " must not grow journal row nodes");
			assertEquals(63, definition.progressLayout().field("var0").maxValue(),
				() -> "quest " + questId + " keeps its legacy 6-bit packed slot");
		}
	}

	@Test
	void killCounterQuestKeepsItsCombinationCounterSlots() throws Exception {
		QuestDefinition definition = definition(KILL_COUNTER).definition();
		assertEquals(35, definition.nodes().size(),
			"1400 keeps its 8x4 kill counter combination (32 combinations + unaccepted/reward/complete)");
		QuestNode reward = node(definition, "reward");
		assertEquals(7, reward.projection().variables().get("var0"),
			"reward var0 is the saturated kill counter, not the journal row");
		assertEquals(3, reward.projection().variables().get("var1"),
			"reward var1 is the saturated kill counter, not the journal row");
		assertEquals(1, definition.transitions().stream()
			.filter(route -> "a7b3".equals(route.sourceNode()) && "reward".equals(route.targetNode()))
			.count(), "the reward route lives on the fully counted combination a7b3");
		assertEquals(Set.of(PAION_OWNER), definition.transitions().stream()
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
			.map(route -> ((QuestEvent.TalkToNpc) route.event()).npcId())
			.collect(Collectors.toSet()), "completion owner stays on Paion");
	}

	private static void assertPrologue(int questId, String zone, int movieId) throws Exception {
		QuestDefinition definition = definition(questId).definition();
		List<QuestTransition> accepts = routes(definition, "unaccepted", "started");
		assertEquals(1, accepts.size(), () -> "quest " + questId + " accept route");
		assertEquals(new QuestEvent.EnterZone(zone), accepts.getFirst().event(),
			() -> "quest " + questId + " is accepted by entering the starting zone");
		List<QuestTransition> replays = routes(definition, "started", "started");
		assertEquals(1, replays.size(), () -> "quest " + questId + " replay route");
		assertEquals(new QuestEvent.EnterZone(zone), replays.getFirst().event(),
			() -> "quest " + questId + " replays the intro on re-entry");
		List<QuestTransition> finishes = routes(definition, "started", "complete");
		assertEquals(1, finishes.size(), () -> "quest " + questId + " completion route");
		assertEquals(new QuestEvent.MovieEnd(movieId), finishes.getFirst().event(),
			() -> "quest " + questId + " completes when the intro movie ends");
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> target.equals(route.targetNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = BlankJournalSlotBoundaryContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
