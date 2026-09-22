package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 37：两条“交谈 → 击杀 → 报告”任务书的整行阶梯（QE-051）。
 * <p>
 * <ul>
 *   <li>26905/26906/26908（Asmodian monster_hunt，三行）：行 0「和 Gangleri/Tyr/Svafnir 对话」、
 *       行 1「消灭 Dark Raider」、行 2「向同一 NPC 报告」。legacy 合同 start_npc_ids=204301/204301/204702、
 *       end_npc_ids=204372/204369/204817：接取 NPC 只管接取，进度与领奖必须落在任务书点名的 end NPC 上。
 *       迁移把 end NPC 的行 0 对话直接写成 started -&gt; reward，reward 投影 0，行 1/2 永远不亮。</li>
 *   <li>3711/4711（Dredgion 舰长，四行）：行 0「和 Mias/Henir 对话」、行 1「搜集德雷得奇安情报」
 *       （730196 术古的 select2 链）、行 2「除掉 DrakanBoss(214823)」、行 3「向 Taranis/Votan 报告」。
 *       legacy 合同是 TALK/REWARD 链：reward 投影必须是行 3，报告入口是 reward + QUEST_SELECT(31) 到
 *       DEFAULT_SUCCESS(10002)，1009 由 npc-complete 预览打开奖励窗。</li>
 * </ul>
 * 两族都补无 source 的 ENTER_WORLD 自愈边（把旧存档的 packed 行 0/1(/2) 推到领奖行），并禁止
 * started -&gt; reward 直跳，防止迁移期“一步领奖”再次回归。
 * <p>
 * Locks batch 37: the two talk/kill/report journal families whose migrated definitions collapsed the
 * middle rows and left the reward projection on row 0.
 */
class Batch37TalkKillReportRowLadderContractTest {

	/** 三行 bounty 族 / Three-row monster-hunt bounty contract. */
	private record Kill3Contract(int questId, int startNpc, int endNpc, Set<Integer> kills) {
	}

	/** 四行 Dredgion 舰长族 / Four-row Dredgion captain contract. */
	private record Talk4Contract(int questId, int startNpc, int firstNpc, int intelNpc, int killNpc) {
	}

	private static final List<Kill3Contract> KILL3_FAMILY = List.of(
		new Kill3Contract(26905, 204301, 204372, Set.of(231555, 231556)),
		new Kill3Contract(26906, 204301, 204369, Set.of(231558, 231559)),
		new Kill3Contract(26908, 204702, 204817, Set.of(231570, 231571))
	);

	private static final List<Talk4Contract> TALK4_FAMILY = List.of(
		new Talk4Contract(3711, 278501, 279045, 730196, 214823),
		new Talk4Contract(4711, 278001, 279042, 730196, 214823)
	);

	private static final List<Integer> ALL_QUEST_IDS = List.of(26905, 26906, 26908, 3711, 4711);

	@Test
	void everyJournalRowOwnsAState() throws Exception {
		for (Kill3Contract contract : KILL3_FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertRow(definition, contract.questId(), "started", 0);
			assertRow(definition, contract.questId(), "t1", 1);
			assertRow(definition, contract.questId(), "k2", 2);
			assertRow(definition, contract.questId(), "reward", 2);
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep a collapsed talk -> reward jump");
		}
		for (Talk4Contract contract : TALK4_FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertRow(definition, contract.questId(), "started", 0);
			assertRow(definition, contract.questId(), "s1", 1);
			assertRow(definition, contract.questId(), "s2", 2);
			assertRow(definition, contract.questId(), "reward", 3);
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep a collapsed talk -> reward jump");
		}
	}

	@Test
	void bountyFamilyAcceptsFromStarterAndReportsToJournalNpc() throws Exception {
		for (Kill3Contract contract : KILL3_FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();

			assertTrue(routes(definition, "unaccepted", "started").stream()
					.anyMatch(route -> talk(route, contract.startNpc())),
				() -> "quest " + contract.questId() + " accepts from the legacy start NPC");

			List<QuestTransition> advance = routes(definition, "started", "t1");
			assertEquals(1, advance.size(),
				() -> "quest " + contract.questId() + " advances row 0 -> 1 exactly once");
			assertEquals(new QuestEvent.TalkToNpc(contract.endNpc(),
				QuestDialogAction.SETPRO1.id(), 0), advance.getFirst().event(),
				() -> "quest " + contract.questId() + " ends the first journal row on the client SETPRO1 button");
			assertTrue(advance.getFirst().conditions().contains(
					new QuestCondition.QuestVariableIs("var0", 0)),
				() -> "quest " + contract.questId() + " gates row 0 on var0=0");
			assertTrue(advance.getFirst().actions().contains(new QuestAction.SetVariable("var0", 1)),
				() -> "quest " + contract.questId() + " writes row 1 on the SETPRO1 route");
			assertTrue(routes(definition, "started", "started").stream()
					.anyMatch(route -> talk(route, contract.endNpc())
						&& route.afterCommit().equals(List.of(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SELECT2.id())))),
				() -> "quest " + contract.questId() + " shows the client select2 page on row 0");

			List<QuestTransition> kill = routes(definition, "t1", "k2");
			assertEquals(1, kill.size(),
				() -> "quest " + contract.questId() + " advances row 1 -> 2 exactly once");
			assertEquals(new QuestEvent.KillNpcSet(contract.kills()), kill.getFirst().event(),
				() -> "quest " + contract.questId() + " counts the client monster-hunt targets");

			assertTrue(routes(definition, "k2", "k2").stream()
					.anyMatch(route -> talk(route, contract.endNpc())
						&& route.afterCommit().equals(List.of(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SELECT5.id())))),
				() -> "quest " + contract.questId() + " shows the client select5 report page on row 2");
			List<QuestTransition> claim = routes(definition, "k2", "reward");
			assertEquals(1, claim.size(),
				() -> "quest " + contract.questId() + " reports row 2 -> reward exactly once");
			assertEquals(new QuestEvent.TalkToNpc(contract.endNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD.id(), 0), claim.getFirst().event(),
				() -> "quest " + contract.questId() + " opens the reward window from the select5 button");
			assertTrue(claim.getFirst().afterCommit().contains(
					new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				() -> "quest " + contract.questId() + " shows the reward window page");

			assertTrue(routes(definition, "reward", "complete").stream()
					.allMatch(route -> talk(route, contract.endNpc())),
				() -> "quest " + contract.questId() + " completes only on the journal end NPC");
			assertTrue(routes(definition, "reward", "complete").stream()
					.noneMatch(route -> talk(route, contract.startNpc())),
				() -> "quest " + contract.questId() + " must not let the starter claim the reward");
		}
	}

	@Test
	void dredgionCaptainFamilyTalksToFirstOfficerAndShugoTrading() throws Exception {
		for (Talk4Contract contract : TALK4_FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();

			List<QuestTransition> firstTalk = routes(definition, "started", "s1");
			assertEquals(1, firstTalk.size(),
				() -> "quest " + contract.questId() + " advances row 0 -> 1 exactly once");
			assertEquals(new QuestEvent.TalkToNpc(contract.firstNpc(),
				QuestDialogAction.SETPRO1.id(), 0), firstTalk.getFirst().event(),
				() -> "quest " + contract.questId() + " ends row 0 on the first officer's SETPRO1 button");
			assertTrue(firstTalk.getFirst().actions().contains(new QuestAction.SetVariable("var0", 1)),
				() -> "quest " + contract.questId() + " writes row 1 after the first officer dialog");

			assertTrue(routes(definition, "s1", "s1").stream()
					.anyMatch(route -> talk(route, contract.intelNpc())
						&& route.afterCommit().equals(List.of(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SELECT2.id())))),
				() -> "quest " + contract.questId() + " shows the shugo select2 page on row 1");
			List<QuestTransition> intel = routes(definition, "s1", "s2");
			assertEquals(1, intel.size(),
				() -> "quest " + contract.questId() + " advances row 1 -> 2 exactly once");
			assertEquals(new QuestEvent.TalkToNpc(contract.intelNpc(),
				QuestDialogAction.SETPRO2.id(), 0), intel.getFirst().event(),
				() -> "quest " + contract.questId() + " ends the intel row on SETPRO2");

			List<QuestTransition> kill = routes(definition, "s2", "reward");
			assertEquals(1, kill.size(),
				() -> "quest " + contract.questId() + " advances row 2 -> reward exactly once");
			assertTrue(isKillOf(kill.getFirst().event(), contract.killNpc()),
				() -> "quest " + contract.questId() + " counts the Dredgion captain kill");

			assertTrue(routes(definition, "reward", "reward").stream()
					.anyMatch(route -> talk(route, contract.startNpc())
						&& route.afterCommit().equals(List.of(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.DEFAULT_SUCCESS.id())))),
				() -> "quest " + contract.questId() + " opens the legacy report entry page on row 3");
			assertTrue(routes(definition, "reward", "complete").stream()
					.allMatch(route -> talk(route, contract.startNpc())),
				() -> "quest " + contract.questId() + " completes on the journal report NPC");
		}
	}

	@Test
	void staleCollapsedRewardSavesHealToTheRewardRow() throws Exception {
		for (Kill3Contract contract : KILL3_FAMILY) {
			assertHealsTo(contract.questId(), 2);
		}
		for (Talk4Contract contract : TALK4_FAMILY) {
			assertHealsTo(contract.questId(), 3);
		}
	}

	private static void assertHealsTo(int questId, int rewardRow) throws Exception {
		CompiledQuestDefinition compiled = definition(questId);
		List<QuestTransition> recoveries = enterWorldRecoveries(compiled.definition());
		for (int staleRow = 0; staleRow < rewardRow; staleRow++) {
			int row = staleRow;
			List<QuestTransition> matches = recoveries.stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", row))))
				.toList();
			assertEquals(1, matches.size(),
				() -> "quest " + questId + " heal route for stale row " + row);
			QuestTransition heal = matches.getFirst();
			assertEquals(List.of(new QuestAction.SetVariable("var0", rewardRow)), heal.actions(),
				() -> "quest " + questId + " heal writes the reward row");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
				() -> "quest " + questId + " heal refreshes the journal");
			assertNull(heal.priority(), () -> "quest " + questId + " heal priority");

			Map<String, Integer> stale = new LinkedHashMap<>(
				compiled.definition().progressLayout().unpack(0));
			stale.put("var0", row);
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot(compiled, stale), heal)
				.orElseThrow(() -> new AssertionError(
					"quest " + questId + " heal plan for stale row " + row));
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + questId + " healed status for row " + row);
			assertEquals(rewardRow, unpack(compiled, plan).get("var0"),
				() -> "quest " + questId + " healed row for stale row " + row);
		}
	}

	private static void assertRow(QuestDefinition definition, int questId, String label, int row) {
		assertEquals(row, node(definition, label).projection().variables().get("var0"),
			() -> "quest " + questId + " node " + label + " row");
	}

	private static boolean talk(QuestTransition route, int npcId) {
		return route.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId;
	}

	private static boolean isKillOf(QuestEvent event, int npcId) {
		return event.equals(new QuestEvent.KillNpc(npcId))
			|| event.equals(new QuestEvent.KillNpcSet(Set.of(npcId)));
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()) && target.equals(route.targetNode()))
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
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
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(variables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = Batch37TalkKillReportRowLadderContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
