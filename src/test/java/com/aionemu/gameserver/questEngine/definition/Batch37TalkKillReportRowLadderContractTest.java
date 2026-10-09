package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 *       legacy 合同是 TALK/REWARD 链：reward 投影 = 原版/legacy 推进值 2（QE-054；行 3 由客户端门槛显示），报告入口是 reward + QUEST_SELECT(31) 到
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
			/* P0c-6 起三行走原版网格合成器直驱（无 IR）：编译视图断言随退役停用，守卫 = 必须确属
			 * 退役（防名单陈旧静默缩水）；网格形（接取态/简报零段/计数满段/领奖）口径由 native 车道门承担。 */
			/* Since P0c-6 the three rows are grid-composed natively (no IR): the compile-view assertions
			 * retire with the XML; the guard keeps the list honest and the native lane gates own the caliber. */
			assertTrue(RetiredQuestIds.contains(contract.questId()),
				() -> "quest " + contract.questId() + " is retail-driven and must be a retired row");
		}
		for (Talk4Contract contract : TALK4_FAMILY) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertRow(definition, contract.questId(), "started", 0);
			assertRow(definition, contract.questId(), "s1", 1);
			assertRow(definition, contract.questId(), "s2", 2);
			assertRow(definition, contract.questId(), "reward", 2);
			assertTrue(routes(definition, "started", "reward").isEmpty(),
				() -> "quest " + contract.questId() + " must not keep a collapsed talk -> reward jump");
		}
	}

	@Test
	void bountyFamilyAcceptsFromStarterAndReportsToJournalNpc() throws Exception {
		for (Kill3Contract contract : KILL3_FAMILY) {
			/* P0c-6：三行走原版网格合成器直驱（无 IR）：本方法原有一整段编译视图断言（简报页/
			 * SETPRO1 清位/击杀计数/报告页/领奖路由）随退役整体停用——守卫 = 必须确属退役
			 * （4ede058c0 重锚漏网段，2026-10-07 收口）；网格形口径由 native 车道门承担。 */
			/* Since P0c-6 the three rows are grid-composed natively (no IR): this method's former
			 * compile-view block (briefing page / SETPRO1 flag clear / kill counters / report page /
			 * claim route) retired with the XML — the guard keeps the list honest (a block left
			 * behind by the 4ede058c0 re-anchoring, closed today); the native lane gates own the
			 * caliber. */
			assertTrue(RetiredQuestIds.contains(contract.questId()),
				() -> "quest " + contract.questId() + " is retail-driven and must be a retired row");
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
			/* P0c-6：原版形状没有任务书行号（var0 是击杀计数，行由客户端 SECTION 门控推导），
			 * 因此不再有也不该有"把旧存档行 0/1 推到领奖行"的无 source 自愈边（P3 既有裁定）；
			 * 退役行无 IR，结构上不可能有修复边——守卫 = 必须确属退役（原 definition() 调用随
			 * 4ede058c0 退役漏网，2026-10-07 收口）。 */
			/* Retail shape has no stored journal row, hence no source-less repair edge may remain;
			 * retired rows have no IR so the edge is structurally impossible — the guard keeps the
			 * list honest (the former definition() call was left behind by 4ede058c0, closed today). */
			assertTrue(RetiredQuestIds.contains(contract.questId()),
				() -> "quest " + contract.questId() + " is retail-driven and must be a retired row");
		}
		/* QE-054：领奖投影 = 原版/legacy 推进值 2（legacy defaultOnKillEvent(214823,2,true) 落盘 2、
		 * 原版集合 {2}），行 3 由客户端 REWARD 态按 [%N] 门槛显示；批次曾误抬为 3。 */
		/* QE-054: the reward projection is the retail/legacy value 2 (legacy kill event persisted 2;
		 * row 3 is shown by the client's own REWARD gate); the batch once mislifted it to 3. */
		for (Talk4Contract contract : TALK4_FAMILY) {
			assertHealsTo(contract.questId(), 2);
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

	/** 击杀边覆盖的 npc 集（KillNpc 单只 / KillNpcSet 家族）。 / Npc targets covered by a kill edge. */
	private static Set<Integer> killTargets(QuestEvent event) {
		return switch (event) {
			case QuestEvent.KillNpc(int npcId) -> Set.of(npcId);
			case QuestEvent.KillNpcSet(Set<Integer> npcIds) -> npcIds;
			default -> Set.of();
		};
	}

	/** 原版单槽网格的目标状态：计数 0/1 + 简报标志位。 / Retail single-slot grid state. */
	private static Map<String, Integer> counters(int kills, boolean briefing) {
		return Map.of("var0", kills, "var5", briefing ? 1 : 0);
	}

	/**
	 * 按 (状态, 投影) 定位原版网格节点标签（网格命名 a0/a1.. 与旧 XML 的阶梯名不同，语义才是契约）。
	 * Semantic (status, projection) node lookup for the retail grid labels.
	 */
	private static String nodeLabel(QuestDefinition definition, int questId, QuestStatus status,
			Map<String, Integer> state) {
		List<String> labels = definition.nodes().stream()
			.filter(node -> node.projection().status() == status)
			.filter(node -> node.projection().variables().equals(state))
			.map(QuestNode::label)
			.toList();
		assertEquals(1, labels.size(), () -> "quest " + questId + " must declare exactly one " + status
			+ " node projecting " + state + ", got " + labels);
		return labels.getFirst();
	}

	private static void assertNode(QuestDefinition definition, int questId, QuestStatus status,
			Map<String, Integer> state, String role) {
		String label = nodeLabel(definition, questId, status, state);
		assertEquals(state, node(definition, label).projection().variables(),
			() -> "quest " + questId + " " + role + " projection");
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

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 原版 overlay）。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}
}
