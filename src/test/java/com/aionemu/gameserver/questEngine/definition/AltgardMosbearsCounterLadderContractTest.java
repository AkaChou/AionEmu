package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 50：2289（Rampaging Mosbears）击杀计数行 + 三行对话阶梯。
 * <p>
 * 判据：客户端 {@code quest_q2289.html} 的 {@code quest_summary} 共 4 行——行 0 = 消灭 MosbearS_13/14
 * （计数槽 {@code [%2]/5}）、行 1 = 回巴斯佩尔特村找 Gefion（203616）、行 2 = 从被关着的 Skanin（203618）
 * 获取情报、行 3 = 杀掉 MosbearNamed_17_An 并把角带回给 Gefion；客户端 {@code quest_script_monster.csv}
 * 把 210564/210584 声明为 {@code Progress(0~4)}（击杀行占 SECTION_0 = 0..4，五次击杀后进 var0=5），
 * 客户端 {@code quest.xml} 的 {@code collect_progress=7} 把收物行钉在 step 7；迁移前 handler
 * {@code _2289RampagingMosbears} 走的就是同一条阶梯（击杀 0-&gt;5、Gefion 5-&gt;6、Skanin 6-&gt;7 并发放
 * Hunter's Secret Remedy 182203017、Gefion 在 7 用 {@code checkQuestItems(7, 7, true, 5, 2120)} 收角进 REWARD）。
 * 迁移把整条阶梯丢掉、reward 投影停在 0，行 1/2/3 永远不亮；本门禁同时锁定 {@code collecting-step=7}，
 * 防止角（182203016）在击杀行就掉。
 * <p>
 * Locks batch 50: the 2289 kill-counter row (SECTION_0 0..4) plus the report/intel/collect ladder, the client
 * page chain including movie 62, the horn-gated hand-over, the single Gefion completion owner and the stale-save
 * heal edges.
 */
class AltgardMosbearsCounterLadderContractTest {

	private static final int QUEST = 2289;
	private static final int GEFION = 203616;
	private static final int SKANIN = 203618;
	private static final int HORN = 182203016;
	private static final int REMEDY = 182203017;
	private static final int REWARD_STEP = 7;
	private static final Set<Integer> MOSBEARS = Set.of(210564, 210584);

	@Test
	void everyClientJournalRowOwnsItsStepState() throws Exception {
		QuestDefinition definition = definition().definition();
		Map<Integer, QuestStatus> steps = new LinkedHashMap<>();
		for (QuestNode node : definition.nodes()) {
			Integer step = node.projection().variables().get("var0");
			QuestStatus status = node.projection().status();
			if (step == null || status != QuestStatus.START && status != QuestStatus.REWARD) {
				continue;
			}
			steps.put(step, status);
		}
		// 行 0 的击杀计数占 0..4，行 1/2/3 依次是 5/6/7；奖励态与收物行同为 step 7。
		for (int step = 0; step <= REWARD_STEP; step++) {
			final int current = step;
			assertEquals(current == REWARD_STEP ? QuestStatus.REWARD : QuestStatus.START, steps.get(current),
				() -> "quest " + QUEST + " step " + current + " must own a journal state");
		}
		assertEquals(REWARD_STEP, node(definition, "reward").projection().variables().get("var0"),
			() -> "quest " + QUEST + " reward projection is the client collect step");
		assertEquals(Map.of("var0", REWARD_STEP),
			definition.progressLayout().unpack(definition.progressLayout().pack(Map.of("var0", REWARD_STEP))),
			() -> "quest " + QUEST + " reward step fits its declared SECTION_0 bit field");
	}

	@Test
	void fiveKillsWalkTheCounterRowIntoTheReportRow() throws Exception {
		QuestDefinition definition = definition().definition();
		String[] sources = {"started", "s1", "s2", "s3", "s4"};
		String[] targets = {"s1", "s2", "s3", "s4", "s5"};
		for (int index = 0; index < sources.length; index++) {
			final int step = index;
			List<QuestTransition> kill = routes(definition, sources[index], targets[index]);
			assertEquals(1, kill.size(), () -> "quest " + QUEST + " kill step " + step + " is a single route");
			QuestTransition route = kill.getFirst();
			assertEquals(new QuestEvent.KillNpcSet(MOSBEARS), route.event(),
				() -> "quest " + QUEST + " counts the client's two MosbearS variants");
			assertTrue(route.conditions().contains(new QuestCondition.QuestVariableIs("var0", step)),
				() -> "quest " + QUEST + " gates kill step " + step + " on its own counter value");
			assertTrue(route.actions().contains(new QuestAction.SetVariable("var0", step + 1)),
				() -> "quest " + QUEST + " accumulates SECTION_0 on kill step " + step);
			// 前四次击杀只回包（计数显示），第 5 次换行时必须刷新可见性。
			List<AfterCommitAction> expected = step == sources.length - 1
				? List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH))
				: List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
			assertEquals(expected, route.afterCommit(),
				() -> "quest " + QUEST + " kill step " + step + " sync mode");
		}
		assertTrue(routes(definition, "s4", "s6").isEmpty(),
			() -> "quest " + QUEST + " must not skip the Gefion report row");
	}

	@Test
	void gefionReportRowShowsClientPagesAndPlaysMovie62() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			singleRoute(definition, "s5", "s5", GEFION, QuestDialogAction.QUEST_SELECT.id()).afterCommit(),
			() -> "quest " + QUEST + " row 1 opens select2(1352)");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			singleRoute(definition, "s5", "s5", GEFION, QuestDialogAction.SELECT2_1.id()).afterCommit(),
			() -> "quest " + QUEST + " row 1 opens select2_1(1353)");
		QuestTransition moviePage = singleRoute(definition, "s5", "s5", GEFION,
			QuestDialogAction.SELECT2_1_1.id());
		assertTrue(moviePage.afterCommit().contains(new AfterCommitAction.PlayMovie(62)),
			() -> "quest " + QUEST + " plays movie 62 before the Skanin briefing");
		assertTrue(moviePage.afterCommit().contains(
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1_1.id())),
			() -> "quest " + QUEST + " keeps the movie's continuation page");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()),
			singleRoute(definition, "s5", "s6", GEFION, QuestDialogAction.SETPRO2.id()).afterCommit(),
			() -> "quest " + QUEST + " row 1 advances on the client SETPRO2(10001) button");
	}

	@Test
	void skaninRowGivesIntelAndTheHuntersRemedy() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())),
			singleRoute(definition, "s6", "s6", SKANIN, QuestDialogAction.QUEST_SELECT.id()).afterCommit(),
			() -> "quest " + QUEST + " row 2 opens select3(1693)");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1.id())),
			singleRoute(definition, "s6", "s6", SKANIN, QuestDialogAction.SELECT3_1.id()).afterCommit(),
			() -> "quest " + QUEST + " row 2 opens select3_1(1694)");
		QuestTransition advance = singleRoute(definition, "s6", "s7", SKANIN, QuestDialogAction.SETPRO3.id());
		assertTrue(advance.actions().contains(new QuestAction.GiveItem(REMEDY, 1)),
			() -> "quest " + QUEST + " hands over Hunter's Secret Remedy 182203017 like legacy");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), advance.afterCommit(),
			() -> "quest " + QUEST + " row 2 advances on the client SETPRO3(10002) button");
	}

	@Test
	void hornHandOverIsGatedByCheckButtonAndClosesOnTheCollectRow() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())),
			singleRoute(definition, "s7", "s7", GEFION, QuestDialogAction.QUEST_SELECT.id()).afterCommit(),
			() -> "quest " + QUEST + " row 3 opens select4(2034)");
		QuestTransition handOver = singleRoute(definition, "s7", "reward", GEFION,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertTrue(handOver.conditions().contains(new QuestCondition.HasItem(HORN, 1)),
			() -> "quest " + QUEST + " requires the horn for the hand-over");
		assertTrue(handOver.actions().contains(new QuestAction.RemoveItem(HORN, 1)),
			() -> "quest " + QUEST + " consumes the horn on the hand-over");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			handOver.afterCommit(), () -> "quest " + QUEST + " opens the client reward window (page 5)");
		assertEquals(0, handOver.priority(), () -> "quest " + QUEST + " gated hand-over wins over the fail page");
		QuestTransition failure = singleRoute(definition, "s7", "s7", GEFION,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertEquals(1, failure.priority(), () -> "quest " + QUEST + " missing-horn branch priority");
		assertTrue(failure.conditions().isEmpty(), () -> "quest " + QUEST + " fail branch has no item gate");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4_2.id())),
			failure.afterCommit(), () -> "quest " + QUEST + " missing-horn branch shows select4_2(2120)");
		assertEquals(List.of(new AfterCommitAction.CloseDialog()),
			singleRoute(definition, "s7", "s7", GEFION, QuestDialogAction.FINISH_DIALOG.id()).afterCommit(),
			() -> "quest " + QUEST + " closes select4_2's only HACTION_FINISH_DIALOG(1008) button");
		assertEquals(REWARD_STEP, dropCollectingStep(definition),
			() -> "quest " + QUEST + " horn only drops on the collect row (collecting-step=7)");
	}

	@Test
	void rewardRowKeepsGefionAsTheOnlyCompletionOwner() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			singleRoute(definition, "reward", "reward", GEFION, QuestDialogAction.QUEST_SELECT.id()).afterCommit(),
			() -> "quest " + QUEST + " reward row reopens the client reward window");
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(route -> "complete".equals(route.targetNode()))
			.toList();
		assertEquals(Set.of(GEFION), talkNpcIds(definition, completions),
			() -> "quest " + QUEST + " completes on Gefion only, never on Skanin");
		List<QuestTransition> previewRoutes = definition.transitions().stream()
			.filter(route -> route.event().equals(
				new QuestEvent.TalkToNpc(GEFION, QuestDialogAction.SELECT_QUEST_REWARD.id())))
			.toList();
		assertEquals(1, previewRoutes.size(),
			() -> "quest " + QUEST + " keeps exactly the npc-complete preview SELECT_QUEST_REWARD route");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), previewRoutes.getFirst().afterCommit(),
			() -> "quest " + QUEST + " preview opens the quest reward window");
		assertEquals(Set.of(GEFION, SKANIN), talkNpcIds(definition),
			() -> "quest " + QUEST + " talk routes cover only Gefion and Skanin");
	}

	@Test
	void staleSavesHealToTheCollectRowAndNeverJumpStraightToReward() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();
		assertTrue(routes(definition, "started", "reward").isEmpty(),
			() -> "quest " + QUEST + " must not keep a collapsed started -> reward jump");
		for (QuestTransition route : definition.transitions()) {
			if (!"reward".equals(route.targetNode())) {
				continue;
			}
			String source = route.sourceNode();
			assertTrue(source == null || "s7".equals(source) || "reward".equals(source),
				() -> "quest " + QUEST + " reward is entered from the collect row only, found " + source);
		}

		QuestTransition rewardHeal = definition.transitions().stream()
			.filter(route -> route.sourceNode() == null)
			.filter(route -> "reward".equals(route.targetNode()))
			.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
			.findFirst().orElseThrow(() -> new AssertionError("quest " + QUEST + " reward heal edge"));
		assertTrue(rewardHeal.conditions().contains(new QuestCondition.StatusIs(QuestStatus.REWARD)),
			() -> "quest " + QUEST + " reward heal only fires in REWARD");
		assertTrue(rewardHeal.conditions().contains(new QuestCondition.QuestVariableIs("var0", 0)),
			() -> "quest " + QUEST + " reward heal covers the migrated var0=0 save");
		assertTrue(rewardHeal.actions().contains(new QuestAction.SetVariable("var0", REWARD_STEP)),
			() -> "quest " + QUEST + " reward heal moves the stale save to step 7");
		QuestMutationPlan healed = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.REWARD, Map.of("var0", 0), Map.of()), rewardHeal).orElseThrow();
		assertEquals(QuestStatus.REWARD, healed.nextStatus(), () -> "quest " + QUEST + " healed status");
		assertEquals(REWARD_STEP, unpack(compiled, healed).get("var0"),
			() -> "quest " + QUEST + " healed journal step");

		QuestTransition hornHeal = definition.transitions().stream()
			.filter(route -> route.sourceNode() == null)
			.filter(route -> "s7".equals(route.targetNode()))
			.filter(route -> route.event().equals(new QuestEvent.EnterWorld()))
			.findFirst().orElseThrow(() -> new AssertionError("quest " + QUEST + " horn heal edge"));
		assertTrue(hornHeal.conditions().contains(new QuestCondition.HasItem(HORN, 1)),
			() -> "quest " + QUEST + " horn heal requires the collect item");
		QuestMutationPlan hornHealed = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.START, Map.of("var0", 0), Map.of(HORN, 1)), hornHeal).orElseThrow();
		assertEquals(QuestStatus.START, hornHealed.nextStatus(), () -> "quest " + QUEST + " horn heal status");
		assertEquals(REWARD_STEP, unpack(compiled, hornHealed).get("var0"),
			() -> "quest " + QUEST + " horn heal moves the save to the collect row");
	}

	private static QuestTransition singleRoute(QuestDefinition definition, String source, String target,
			int npcId, int actionId) {
		List<QuestTransition> matches = routes(definition, source, target).stream()
			.filter(route -> route.event().equals(new QuestEvent.TalkToNpc(npcId, actionId)))
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + QUEST + " route " + source + " -> " + target
			+ " on npc " + npcId + " action " + actionId);
		return matches.getFirst();
	}

	private static Set<Integer> talkNpcIds(QuestDefinition definition) {
		return talkNpcIds(definition, definition.transitions());
	}

	private static Set<Integer> talkNpcIds(QuestDefinition definition, List<QuestTransition> routes) {
		Set<Integer> npcIds = new LinkedHashSet<>();
		for (QuestTransition route : routes) {
			if (route.event() instanceof QuestEvent.TalkToNpc talk) {
				npcIds.add(talk.npcId());
			}
		}
		return npcIds;
	}

	private static int dropCollectingStep(QuestDefinition definition) {
		return definition.metadata().drops().stream()
			.filter(drop -> drop.npcId() == 210442 && drop.itemId() == HORN)
			.findFirst().orElseThrow(() -> new AssertionError("quest " + QUEST + " horn drop")).collectingStep();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> Objects.equals(source, route.sourceNode()) && target.equals(route.targetNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), inventory);
	}

	private static CompiledQuestDefinition definition() throws IOException {
		try (InputStream input = AltgardMosbearsCounterLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + QUEST + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + QUEST + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
