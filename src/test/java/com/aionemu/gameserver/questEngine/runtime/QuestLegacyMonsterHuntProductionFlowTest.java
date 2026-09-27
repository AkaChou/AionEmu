package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证旧 monster-hunt 模板的末次击杀、独立计数和报告 owner 合同。
 * Verifies final-kill, independent-counter, and report-owner contracts from the legacy monster-hunt template.
 */
class QuestLegacyMonsterHuntProductionFlowTest {
	private static final Set<Integer> QUEST_1842_REGULAR_TARGETS = Set.of(
		215094, 215095, 215096, 215097, 215098, 215099, 215100, 215101, 215102, 215103,
		215104, 215105, 215106, 215107, 215108, 215109, 215110, 215111, 215112, 215113,
		215114, 215115, 215116, 215117, 215118, 215119, 215120, 215121, 215122, 215123,
		215124, 215125, 215126, 215127, 215128, 215129, 215130, 215131, 215132, 215133,
		215135, 215136, 215285, 215286, 215287, 215288, 215289, 215290, 215291, 215292,
		215293, 215294, 215295, 215296, 215297, 215298, 215299, 215300, 215301, 215302,
		215303, 215304, 215305, 215306, 215307, 215308, 215309, 215310, 215311, 215312,
		215313, 215314, 215315, 215316);
	private static final Set<Integer> QUEST_18951_TARGETS = IntStream.rangeClosed(236100, 236220)
		.boxed().collect(Collectors.toUnmodifiableSet());
	private static final List<MonsterHuntContract> SIMPLE_HUNTS = List.of(
		new MonsterHuntContract(18314, Set.of(702656, 730373), 730373, 7),
		new MonsterHuntContract(18951, QUEST_18951_TARGETS, 236100, 25),
		new MonsterHuntContract(18972, Set.of(235824, 235825), 235824, 6),
		new MonsterHuntContract(18973, Set.of(235867, 235868), 235867, 6),
		new MonsterHuntContract(18974, Set.of(235881), 235881, 6));
	private static final List<ReportedMonsterHuntContract> REPORTED_HUNTS = List.of(
		new ReportedMonsterHuntContract(29631, Set.of(214419, 214420, 214433, 214434),
			205150, 205150, 6242224, 114101722),
		new ReportedMonsterHuntContract(29632, Set.of(214431, 214432, 214542),
			205150, 205150, 6242224, 113101688),
		new ReportedMonsterHuntContract(29633, Set.of(214408, 214429, 214430),
			205150, 205164, 6242224, 111101676),
		new ReportedMonsterHuntContract(29634, Set.of(214371, 214372, 214440, 214441),
			205164, 205164, 6242224, 110101862),
		new ReportedMonsterHuntContract(29635, Set.of(214482, 214483, 214486, 214487),
			205164, 205164, 6242224, 112101626),
		new ReportedMonsterHuntContract(29636, Set.of(214489, 214490, 214491, 214492),
			205164, 205150, 6242224, 100001774),
		new ReportedMonsterHuntContract(29637, Set.of(215879, 215880, 215937),
			799225, 799248, 6937236, 114101721),
		new ReportedMonsterHuntContract(29638, Set.of(215907, 215918, 215919),
			799248, 799248, 6937236, 113101687),
		new ReportedMonsterHuntContract(29639, Set.of(215988, 215989),
			799248, 799248, 6937236, 111101675),
		new ReportedMonsterHuntContract(29640, Set.of(215992, 215995),
			799248, 799248, 6937236, 110101861),
		new ReportedMonsterHuntContract(29641, Set.of(215942, 216045, 216046),
			799248, 799297, 6937236, 112101625),
		new ReportedMonsterHuntContract(29642, Set.of(215888, 215889, 216009, 216010),
			799297, 799225, 6937236, 100001773),
		new ReportedMonsterHuntContract(30516, Set.of(236300),
			805156, 799670, 1, 3568486, 186000236, 5),
		new ReportedMonsterHuntContract(27160, Set.of(219700, 219777, 219788),
			804719, 804719, 10, 4825175, 186000199),
		new ReportedMonsterHuntContract(27161, Set.of(235832, 235914, 235918),
			804719, 804719, 10, 4825175, 186000199),
		new ReportedMonsterHuntContract(30708, Set.of(800425, 800426, 800427),
			800369, 800438, 5, 7086913, 186000201),
		new ReportedMonsterHuntContract(30758, Set.of(800425, 800426, 800427),
			800369, 800438, 5, 7086913, 186000201));
	/**
	 * 顺序 SECTION 链契约（客户端 {@code quest_monster.csv} 链式门控
	 * {@code Progress(SECTION_i==count; SECTION_(i+1)<count)}）：前序段满后客户端才切换下一段清单，
	 * 因此节点集 = 链式前缀状态、击杀边只推进首个未满段，乱序/后续段目标不计数。
	 * Sequential SECTION-chain contracts (the client's chained gates): the journal switches to the
	 * next stage's list only after the previous section completes, so nodes are chained prefix
	 * states, kill edges advance the first unfinished stage, and later-stage targets never count.
	 */
	private static final List<SequentialChainContract> SEQUENTIAL_CHAINS = List.of(
		new SequentialChainContract(30514, 799592, 799670, List.of(1, 1),
			List.of(Set.of(217310), Set.of(217317)),
			List.of(new QuestAction.GrantReward("GOLD", 0, 598320, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 5097837, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("TITLE", 211, 1, QuestRewardAmountMode.EXACT))),
		new SequentialChainContract(30564, 799592, 799670, List.of(1, 1),
			List.of(Set.of(217310), Set.of(217317)),
			List.of(new QuestAction.GrantReward("GOLD", 0, 598320, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 5097837, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("TITLE", 222, 1, QuestRewardAmountMode.EXACT))),
		new SequentialChainContract(25406, 805401, 805401, List.of(4, 4),
			List.of(Set.of(883644), Set.of(883645)),
			List.of(new QuestAction.GrantReward("EXP", 0, 53023500, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000237, 23, QuestRewardAmountMode.EXACT))),
		new SequentialChainContract(25407, 805401, 805401, List.of(4, 4),
			List.of(Set.of(883646), Set.of(883648)),
			List.of(new QuestAction.GrantReward("EXP", 0, 53023500, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000237, 23, QuestRewardAmountMode.EXACT))),
		new SequentialChainContract(25408, 805401, 805401, List.of(4, 4),
			List.of(Set.of(883909), Set.of(883911)),
			List.of(new QuestAction.GrantReward("EXP", 0, 53023500, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000237, 23, QuestRewardAmountMode.EXACT))),
		new SequentialChainContract(25580, 806116, 806116, List.of(20, 20),
			List.of(Set.of(241246, 241480, 241484, 241488), Set.of(241247, 241481, 241485, 241489)),
			List.of(new QuestAction.GrantReward("EXP", 0, 91465537, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000414, 2, QuestRewardAmountMode.EXACT))),
		new SequentialChainContract(17541, 799553, 799553, List.of(1, 1, 1),
			List.of(Set.of(217185), Set.of(217195), Set.of(217204, 217206)),
			// M2-c 显示名族展开后，246160/246161/246196/246261 是同名合法变体并计入对应段；
			// 只有 217205 不属于任何段显示名。
			// After the M2-c display-name family expansion the 246xxx ids are legitimate same-name
			// variants counted in their stages; only 217205 maps to no stage display name.
			Set.of(217205),
			List.of(new QuestAction.GrantReward("EXP", 0, 2814541, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 162000050, 10, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 164000070, 10, QuestRewardAmountMode.EXACT))),
		new SequentialChainContract(27541, 799558, 799558, List.of(1, 1, 1),
			List.of(Set.of(217185), Set.of(217195), Set.of(217204, 217206)),
			Set.of(217205),
			List.of(new QuestAction.GrantReward("EXP", 0, 2814541, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 162000050, 10, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 164000070, 10, QuestRewardAmountMode.EXACT))),
		new SequentialChainContract(25060, 804730, 804730, List.of(1, 1, 1),
			List.of(),
			List.of(new QuestAction.GrantReward("GOLD", 0, 110340, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 9492173, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000231, 30, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 186000237, 25, QuestRewardAmountMode.EXACT))));

	@TestFactory
	Stream<DynamicTest> simpleMonsterHuntsReachRewardOnTheRetailFinalKill() {
		return SIMPLE_HUNTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertSimpleMonsterHunt(contract)));
	}

	@TestFactory
	Stream<DynamicTest> reportedMonsterHuntsRequireTheRetailKillCountBeforeTurnIn() {
		return REPORTED_HUNTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertReportedMonsterHunt(contract)));
	}

	@TestFactory
	Stream<DynamicTest> sequentialSectionChainsAdvanceOnlyTheFirstUnfinishedStage() {
		return SEQUENTIAL_CHAINS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertSequentialSectionChain(contract)));
	}

	@Test
	void quest17541WalksThreeSequentialSectionKillsBeforeTheRetailReport() throws Exception {
		assertSequentialSectionChain(SEQUENTIAL_CHAINS.stream()
			.filter(contract -> contract.questId() == 17541).findFirst().orElseThrow());
	}

	@Test
	void quest27541WalksThreeSequentialSectionKillsBeforeTheRetailReport() throws Exception {
		assertSequentialSectionChain(SEQUENTIAL_CHAINS.stream()
			.filter(contract -> contract.questId() == 27541).findFirst().orElseThrow());
	}

	@Test
	void quest25060WalksThreeSequentialSectionKillsWithTheClientReportPages() throws Exception {
		assertSequentialSectionChain(SEQUENTIAL_CHAINS.stream()
			.filter(contract -> contract.questId() == 25060).findFirst().orElseThrow());
	}

	@Test
	void quests25090And25093UseTheClientSuccessPageWhileObjectivesAreIncomplete() throws Exception {
		for (int questId : List.of(25090, 25093)) {
			QuestDefinition definition = load(questId).definition();
			// DD 网格行：接取直落零段；未满段不再有完成页自环（P0-2 规范形）。
			// Grid rows: the accept lands on the zero segment; the incomplete self-loop is gone
			// (canonical since P0-2).
			String source = gridNode(definition, 0).label();
			assertIncompleteSegmentKeepsNoReportChannel(questId, definition, source);
		}
	}

	@Test
	void simpleMonsterHuntsUseTheClientSuccessPageWhileObjectivesAreIncomplete() throws Exception {
		for (Map.Entry<Integer, Integer> entry : Map.ofEntries(
			Map.entry(25002, 804903),
			Map.entry(25010, 804721),
			Map.entry(25201, 804914),
			Map.entry(25202, 804914),
			Map.entry(25203, 804914),
			Map.entry(25325, 805343),
			Map.entry(25409, 805402),
			Map.entry(25410, 805402),
			Map.entry(25411, 805402),
			Map.entry(25412, 805402),
			Map.entry(25413, 805402),
			Map.entry(25414, 805408),
			Map.entry(25415, 805409),
			Map.entry(25416, 805410),
			Map.entry(25417, 805411),
			Map.entry(25418, 805412),
			Map.entry(25419, 805413),
			Map.entry(25420, 805414),
			Map.entry(25421, 805415),
			Map.entry(25422, 805416),
			Map.entry(25423, 805417),
			Map.entry(25424, 805418),
			Map.entry(25425, 805419),
			Map.entry(25426, 805420),
			Map.entry(25427, 805421),
			Map.entry(25428, 805422),
			Map.entry(25429, 805423),
			Map.entry(25430, 805408),
			Map.entry(25431, 805409),
			Map.entry(25432, 805410),
			Map.entry(25433, 805411),
			Map.entry(25434, 805412),
			Map.entry(25435, 805413),
			Map.entry(25436, 805414),
			Map.entry(25437, 805415),
			Map.entry(25438, 805416),
			Map.entry(25439, 805417),
			Map.entry(25440, 805418),
			Map.entry(25441, 805419),
			Map.entry(25442, 805420),
			Map.entry(25443, 805421),
			Map.entry(25444, 805422),
			Map.entry(25445, 805423),
			Map.entry(25471, 805815),
			Map.entry(25472, 805815),
			Map.entry(25473, 805815),
			Map.entry(25474, 805815),
			Map.entry(25475, 805815)).entrySet()) {
			QuestDefinition definition = load(entry.getKey()).definition();
			// 网格行按零段寻址；XML 行仍是 started 自环。规范形网格行已无完成页自环（P0-2），
			// 遗留形（XML 保留/顺序链）保留同一客户端语义。
			// Grid rows address the zero segment; XML rows keep the started self-loop. Canonical
			// grid rows lost the success-page self-loop (P0-2); legacy shapes keep it.
			String source = isGridShaped(definition) ? gridNode(definition, 0).label() : "started";
			assertIncompleteSegmentKeepsNoReportChannel(entry.getKey(), definition, source);
		}
	}

	/**
	 * 未满段的报告通道按形状二分（P0-2 规范形）：网格规范行无 QUEST_SELECT/1009 路由（页链不再由
	 * 服务端驱动，FINISH_DIALOG 关窗出口保留）；遗留形（XML 保留/顺序链）保留客户端完成页自环。
	 * The incomplete segment's report channel splits by shape (canonical since P0-2): canonical grid
	 * rows carry no QUEST_SELECT/1009 routes (pages are no longer server-driven; the FINISH_DIALOG
	 * close exit stays); legacy shapes (XML retention / sequential chains) keep the success-page
	 * self-loop.
	 */
	private static void assertIncompleteSegmentKeepsNoReportChannel(int questId, QuestDefinition definition,
			String source) {
		List<QuestTransition> selfLoops = definition.transitions().stream()
			.filter(t -> source.equals(t.sourceNode()) && source.equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
			.toList();
		if (selfLoops.isEmpty()) {
			assertTrue(definition.transitions().stream().noneMatch(t ->
					source.equals(t.sourceNode()) && t.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> "quest " + questId + " 未满段不得保留报告通道路由");
			return;
		}
		assertEquals(1, selfLoops.size(), () -> "quest " + questId + " 未满段完成页自环");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			selfLoops.getFirst().afterCommit());
	}

	/** 挑战任务哨兵轴（P0c-58）：接取人 = 真端 reward 名解析出的 NPC，两侧各一；10 杀单段网格。 */
	private record ChallengeHunt(int acceptNpc, Set<Integer> targets) {
	}

	/**
	 * 挑战任务哨兵轴（P0c-58，2026-09-26）：真端 DD 表的 `value0_acquire_ = _challengetask_` 是哨兵而非
	 * NPC 名，接取人回退到真端 reward 名（客户端 npc 块在该 NPC 上声明的 `quest_ai_name`）⇒ 接取人 =
	 * 交付人：光侧 804699（17160/17161）、暗侧 804719（27160/27161）。旧的 legacy 计数边（`started` 自环
	 * `KillNpcSet` 带 priority）与 `reward` 态重开页在真端形里换成了击杀网格分段（a&lt;kills&gt;，见
	 * {@link #assertGridReportedMonsterHunt}）与饱和段的成功页；本方法锁**客户端接取页 + 报告页**两条真端页边
	 * （测试名所指），其余由同族网格合同覆盖。
	 * <p>
	 * Challenge-task sentinel axis (P0c-58): the retail sentinel `_challengetask_` falls back to the retail
	 * reward name, which the client npc block declares as that npc's dialog routing name — so the accept npc
	 * is the hand-in npc (804699 light / 804719 dark). The legacy counted `KillNpcSet` edges and the
	 * reward-state re-open page are replaced by the kill-counter grid segments and the saturated segment's
	 * success page; this method locks the two retail page edges its name refers to.
	 */
	private static final Map<Integer, ChallengeHunt> CHALLENGE_HUNTS = Map.of(
		17160, new ChallengeHunt(804699, Set.of(235830, 235912, 235916)),
		17161, new ChallengeHunt(804699, Set.of(219699, 219776, 219787)),
		27160, new ChallengeHunt(804719, Set.of(219700, 219777, 219788)),
		27161, new ChallengeHunt(804719, Set.of(235832, 235914, 235918)));

	@Test
	void challengeMonsterHuntsUseTheRetailClientAcceptAndReportPages() throws Exception {
		for (int questId : List.of(17160, 17161, 27160, 27161)) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			int acceptNpc = CHALLENGE_HUNTS.get(questId).acceptNpc();
			QuestNode zero = gridNode(definition, 0);
			QuestNode full = gridNode(definition, 10);
			QuestNode reward = nodeByProjection(definition, QuestStatus.REWARD, Map.of("var0", 10));

			// 接取页：未接态的 QUEST_SELECT 直发接取窗（页 4；P0-2 规范形，客户端入口页不再由服务端下发）。
			// Accept page: the unaccepted QUEST_SELECT emits the ask window (page 4; canonical since
			// P0-2, the client entry page is no longer server-driven).
			QuestTransition entryPage = transition(definition, "unaccepted", "unaccepted",
				new QuestEvent.TalkToNpc(acceptNpc, QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(), entryPage.conditions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), entryPage.afterCommit());

			// 接取边：ACCEPT_SIMPLE(20000) 直落网格零段（"接取人 = 交付人"的真端形）。
			// Accept edge: ACCEPT_SIMPLE lands on the zero segment (accept npc == hand-in npc).
			QuestTransition accept = transition(definition, "unaccepted", zero.label(),
				new QuestEvent.TalkToNpc(acceptNpc, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
			assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), accept.afterCommit());

			// 击杀步：三个目标任一都同构推进一段（空条件/动作，计数即节点）。
			// Kill steps: any of the three targets advances one segment identically.
			for (int npcId : CHALLENGE_HUNTS.get(questId).targets()) {
				QuestTransition step = transition(definition, zero.label(), gridNode(definition, 1).label(),
					new QuestEvent.KillNpc(npcId));
				assertEquals(List.of(), step.conditions());
				assertEquals(List.of(), step.actions());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					step.afterCommit());
			}

			// 交付：满段 QUEST_SELECT 直翻 REWARD 并按档位查表下发奖励窗（P0-2 规范形；
			// 客户端完成页与 1009 中转不再由服务端驱动，未满段零对话路由）。
			// Delivery: the full node's QUEST_SELECT flips REWARD with the tiered reward window
			// (canonical since P0-2; the client success page and the 1009 hop are no longer
			// server-driven, and incomplete segments carry no dialog routes).
			QuestTransition deliver = transition(definition, full.label(), reward.label(),
				new QuestEvent.TalkToNpc(acceptNpc, QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(), deliver.conditions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
					definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
				deliver.afterCommit());
			assertNoMatch(compiled, snapshot(questId, QuestStatus.START, Map.of("var0", 0), definition),
				new QuestEvent.TalkToNpc(acceptNpc, QuestDialogAction.QUEST_SELECT.id()));
		}
	}

	@Test
	void quest1842PreservesIndependentObjectivesAndReportsToTheRetailEndNpc() throws Exception {
		CompiledQuestDefinition compiled = load(1842);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, Map.of("var0", 80, "var1", 1));
		assertQuest1842Routes(definition);

		assertQuest1842Order(compiled, true);
		assertQuest1842Order(compiled, false);
		assertReport(definition, "ready", 278503);
	}

	@Test
	void quest21120ReachesTurnInOnTheTenthKill() throws Exception {
		CompiledQuestDefinition compiled = load(21120);
		QuestDefinition definition = compiled.definition();
		/* P0c-8c（2026-09-24）：21120 已由真端 SimpleHunt 表驱动（a0..a10 十段击杀网格，var0 = 击杀数，
		   layout 由 4 位 max10 重基到真端 6 位 max63）。旧 XML 的 `started` 自环 + `VariableBelow(var0, 9)`
		   是同一语义的另一种表示；网格步进条件/动作全空（计数即节点打包值），饱和段自身即报告门控。
		   Retail-driven since P0c-8c: the grid steps carry no counter conditions, so the step assertions move
		   to the grid's first and saturated segments instead of the legacy counter edge. */
		List<QuestNode> steps = startStepsByPack(definition);
		assertEquals(Map.of("var0", 10), steps.getLast().projection().variables());
		for (int npcId : List.of(216102, 216103)) {
			QuestTransition step = transition(definition, steps.getFirst().label(), steps.get(1).label(),
				new QuestEvent.KillNpc(npcId));
			assertEquals(List.of(), step.conditions());
			assertEquals(List.of(), step.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				step.afterCommit());
		}

		QuestEvent event = new QuestEvent.KillNpc(216102);
		QuestSnapshot snapshot = snapshot(21120, QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= 10; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, event);
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count), definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		assertNoMatch(compiled, snapshot, event);
		/* 规范形交付（quest-native-dispatch P0-2）：满段 QUEST_SELECT 直翻 REWARD 并按档位下发奖励窗
		   （页 5）；客户端报告页与 1009 中转不再由服务端驱动。
		   Canonical delivery (quest-native-dispatch P0-2): the full node's QUEST_SELECT flips REWARD and
		   shows the tiered reward window (page 5); the client report page and the 1009 hop are no longer
		   server-driven. */
		QuestTransition deliver = transition(definition, steps.getLast().label(), "reward",
			new QuestEvent.TalkToNpc(799291, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), deliver.conditions());
		assertEquals(List.of(), deliver.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			deliver.afterCommit());
		QuestMutationPlan report = dispatch(compiled, snapshot,
			new QuestEvent.TalkToNpc(799291, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", 10), definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	@Test
	void quest26988CountsFiveKillsBeforeRetailReportAndRewardsAt804867() throws Exception {
		CompiledQuestDefinition compiled = load(26988);
		QuestDefinition definition = compiled.definition();
		/* P5-1 网格 + P0-2 规范形（2026-09-26）：26988 由真端 DataDriven 击杀网格驱动（a0..a5 单段，
		   var0 = 击杀数，段进位无条件/动作）。旧 XML 的 var0 完成 flag + 条件计数边是同一语义的另一表示；
		   满段 QUEST_SELECT 直接翻 REWARD 并按档位查表下发奖励窗——客户端完成页与 1009 中转不再由
		   服务端驱动，未满段无 QUEST_SELECT/1009 报告通道。
		   Grid-driven since P5-1 and canonical since P0-2: the full node's QUEST_SELECT flips REWARD
		   with the tiered window; the client success page and the 1009 hop are no longer server-driven
		   and incomplete segments keep no QUEST_SELECT/1009 report channel. */
		QuestNode zero = gridNode(definition, 0);
		QuestNode full = gridNode(definition, 5);
		QuestNode reward = nodeByProjection(definition, QuestStatus.REWARD, Map.of("var0", 5));

		QuestEvent stepEvent = new QuestEvent.KillNpc(233126);
		QuestTransition step = transition(definition, zero.label(), gridNode(definition, 1).label(), stepEvent);
		assertEquals(List.of(), step.conditions());
		assertEquals(List.of(), step.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			step.afterCommit());

		// 末杀与中途步同构：饱和段 a5 仍是 START，满段交付才进领奖——计数即节点，饱和段自身即交付门控。
		// The final kill is edge-identical to mid steps: segment a5 stays START; only the full-node
		// delivery enters reward — the counter is the node, the saturated segment gates delivery.
		QuestSnapshot snapshot = snapshot(26988, QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= 5; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, stepEvent);
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		assertNoMatch(compiled, snapshot, stepEvent);

		// 未满段（a3）：无 QUEST_SELECT/1009 报告通道（页链不再由服务端驱动）。
		// Incomplete segment a3: no QUEST_SELECT/1009 report channel (pages are no longer
		// server-driven).
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			gridNode(definition, 3).label().equals(candidate.sourceNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
			() -> "retail 26988 incomplete segment must keep no report channel");

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(804867, QuestDialogAction.QUEST_SELECT.id());
		QuestTransition report = transition(definition, full.label(), reward.label(), reportEvent);
		assertEquals(List.of(), report.conditions());
		assertNull(report.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
			report.afterCommit());
		QuestMutationPlan reportPlan = dispatch(compiled,
			snapshot(26988, QuestStatus.START, Map.of("var0", 5), definition), reportEvent);
		assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());

		// 真端 hunt 行在 REWARD 态无 QUEST_SELECT 重开路由（P0c-8c 普查 transXmlOnly 判例）：
		// 只有 1009/USE_OBJECT 预览再次开窗。
		// Retail hunt rows keep no reward-state QUEST_SELECT re-open (P0c-8c transXmlOnly); only the
		// 1009/USE_OBJECT previews re-open the window.
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			reward.label().equals(candidate.sourceNode()) && reward.label().equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			() -> "retail 26988 must not keep the legacy reward-state re-open route");
		for (QuestDialogAction action : List.of(QuestDialogAction.USE_OBJECT,
				QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = transition(definition, "reward", "reward",
				new QuestEvent.TalkToNpc(804867, action.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(804867, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 3618881, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000236, 5, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
		assertTrue(definition.transitions().stream().noneMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 804865));
	}

	@Test
	void quest29691RequiresThreeKillsBeforeTheRetailReport() throws Exception {
		CompiledQuestDefinition compiled = load(29691);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 3));

		Set<Integer> targetNpcIds = Set.of(
			246200, 246201, 246202, 246203, 246204, 246205, 246206, 246207, 246208, 246209,
			246210, 246211, 246212, 246213, 246214, 246215, 246216, 246217, 246218, 246219,
			246220, 246221, 246222, 246223, 246224, 246225, 246226, 246227, 246228, 246229,
			246230, 246231, 246232, 246233, 246234, 246235, 246236, 246237, 246238, 246239,
			248037, 248038, 248039, 248040, 248041, 248042, 248043, 248044, 248045, 248046,
			248047, 248048, 248049, 248050, 248051, 248052, 248053, 248054, 248055, 248056,
			248057, 248058, 248059, 248060, 248061, 248062, 248063, 248064, 248065, 248066,
			248067, 248068, 248069, 248070, 248071, 248072, 248073, 248074, 248075, 248076);
		QuestEvent targets = new QuestEvent.KillNpcSet(targetNpcIds);
		QuestTransition continuing = transition(definition, "started", "started", targets, 1);
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", 2)), continuing.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 0), new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition finalKill = transition(definition, "started", "reward", targets, 0);
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 2)), finalKill.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1), new QuestAction.SetVariable("var1", 3)), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			finalKill.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(806700,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(29691, QuestStatus.START, Map.of("var0", 0, "var1", 0), definition);
		for (int count = 1; count <= 3; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(246200));
			snapshot = nextSnapshot(snapshot, plan);
			if (count < 3) {
				assertEquals(QuestStatus.START, snapshot.status());
				assertEquals(Map.of("var0", 0, "var1", count),
					definition.progressLayout().unpack(snapshot.packedVariables()));
				assertNoMatch(compiled, snapshot, reportEvent);
			} else {
				assertEquals(QuestStatus.REWARD, snapshot.status());
				assertEquals(Map.of("var0", 1, "var1", 3),
					definition.progressLayout().unpack(snapshot.packedVariables()));
			}
		}
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(246200));

		QuestTransition reopen = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(806700, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reopen.afterCommit());
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(806700, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(806700, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 250000000, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	@Test
	void quest30005RequiresTwentyFiveKillsBeforeTheRetailReport() throws Exception {
		CompiledQuestDefinition compiled = load(30005);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, Map.of("var0", 25));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 25));

		Set<Integer> targetNpcIds = Set.of(
			215808, 215809, 215814, 215857, 215858,
			216327, 216328, 216336, 216343, 216344);
		QuestEvent targets = new QuestEvent.KillNpcSet(targetNpcIds);
		QuestTransition continuing = transition(definition, "started", "started", targets);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 24)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition finalKill = transition(definition, "started", "ready", targets);
		assertEquals(0, finalKill.priority());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 24)), finalKill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			finalKill.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(799029,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(30005, QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= 25; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(215808));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
			if (count < 25) {
				assertNoMatch(compiled, snapshot, reportEvent);
			}
		}
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(215808));

		QuestTransition reportPage = transition(definition, "ready", "ready",
			new QuestEvent.TalkToNpc(799029, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			reportPage.afterCommit());
		QuestTransition report = transition(definition, "ready", "reward", reportEvent);
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, reportEvent));
		assertEquals(QuestStatus.REWARD, snapshot.status());
		assertEquals(Map.of("var0", 25), definition.progressLayout().unpack(snapshot.packedVariables()));

		QuestTransition reopen = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(799029, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			reopen.afterCommit());
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(799029, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(799029, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 24000, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 4397266, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000095, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	@Test
	void quest30715DeliversStraightToTheTieredRewardWindowAfterTheRetailKill() throws Exception {
		CompiledQuestDefinition compiled = load(30715);
		QuestDefinition definition = compiled.definition();
		/* P0c-8c（2026-09-24）：30715 已由真端 SimpleHunt 表驱动（a0 --击杀--> a1 两段网格）；P0-2 规范形后
		   满段 QUEST_SELECT 直翻 REWARD，客户端报告页 2375 与 1009 中转不再由服务端驱动。
		   旧 XML 的 `started`/`ready` 标签与 `VariableBelow(var0, 1)` 计数条件被真端网格取代；REWARD 状态在
		   真端只保留 1009 预览路由，旧 XML 的 `reward --31--> reward`（重开报告页）在真端表中不存在，
		   属 P0c-8c 普查登记并采纳的遗留多余路由（transXmlOnly dialogId=31），此处作为契约正向锁定。
		   Retail-driven since P0c-8c; canonical since P0-2: the full node's QUEST_SELECT flips REWARD and
		   the client report page / 1009 hop are no longer server-driven. The legacy reward-state
		   QUEST_SELECT re-open route stays absent — locked here as adopted. */
		QuestNode reportState = startStepsByPack(definition).getLast();
		assertEquals(Map.of("var0", 1), reportState.projection().variables());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));

		QuestEvent target = new QuestEvent.KillNpc(219357);
		QuestTransition kill = transition(definition, startStepsByPack(definition).getFirst().label(),
			reportState.label(), target);
		assertEquals(List.of(), kill.conditions());
		assertEquals(List.of(), kill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			kill.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(804870,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(30715, QuestStatus.START, Map.of("var0", 0), definition);
		// 规范形删除满段 1009 上交（含未满段）：报告页不再由服务端驱动。
		// Canonical removed the full-node 1009 hand-in: the report page is no longer server-driven.
		assertNoMatch(compiled, snapshot, reportEvent);
		snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, target));
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(Map.of("var0", 1), definition.progressLayout().unpack(snapshot.packedVariables()));
		assertNoMatch(compiled, snapshot, target);

		// 规范形交付：满段 QUEST_SELECT 直翻 REWARD 并按档位下发奖励窗（页 5）。
		// Canonical delivery: the full node's QUEST_SELECT flips REWARD and shows the tiered window.
		QuestTransition deliver = transition(definition, reportState.label(), "reward",
			new QuestEvent.TalkToNpc(804870, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), deliver.conditions());
		assertEquals(List.of(), deliver.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			deliver.afterCommit());
		snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot,
			new QuestEvent.TalkToNpc(804870, QuestDialogAction.QUEST_SELECT.id())));
		assertEquals(QuestStatus.REWARD, snapshot.status());

		// 真端在 REWARD 状态不含 QUEST_SELECT 重开路由：只有 1009 预览会再次展示同一页面。
		// Retail keeps no reward-state QUEST_SELECT re-open route; only the 1009 preview re-shows the page.
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			"reward".equals(candidate.sourceNode()) && "reward".equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			() -> "retail 30715 must not keep the legacy reward-state re-open route");
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(804870, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(804870, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 662040, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 7086913, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertReportedMonsterHunt(ReportedMonsterHuntContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		if (definition.nodes().stream().anyMatch(n -> "ready".equals(n.label()))) {
			assertLegacyReportedMonsterHunt(compiled, contract);
			return;
		}
		// 旧双变量形（var0 完成 flag + var1 计数）才走 legacy 断言；网格行（含带简报行：
		// started + var5 标志位 + var0 单计数段）一律走网格合同。var1 只出现在领奖节点投影
		// （started 投影为空），故按任意节点判定。
		// Only the legacy two-var shape takes the legacy assertions; grid rows — including briefing
		// rows (started + var5 flag + var0 counter section) — take the grid contract. var1 may only
		// appear on the reward-node projection (started is empty), so match any node.
		boolean legacyTwoVar = definition.nodes().stream().anyMatch(node ->
			node.projection().variables().containsKey("var1")
				&& node.projection().variables().size() == 2);
		if (!legacyTwoVar) {
			assertGridReportedMonsterHunt(compiled, contract);
			return;
		}
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", contract.requiredKills()));

		QuestEvent targets = new QuestEvent.KillNpcSet(contract.targetNpcIds());
		QuestTransition finalKill;
		if (contract.requiredKills() == 1) {
			finalKill = transition(definition, "started", "reward", targets, 0);
			assertEquals(List.of(new QuestCondition.VariableBelow("var1", 1)), finalKill.conditions());
		} else {
			QuestTransition continuing = transition(definition, "started", "started", targets, 1);
			assertEquals(List.of(new QuestCondition.VariableBelow("var1", contract.requiredKills() - 1)),
				continuing.conditions());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 0), new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				continuing.afterCommit());
			finalKill = transition(definition, "started", "reward", targets, 0);
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", contract.requiredKills() - 1)),
				finalKill.conditions());
		}
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1), new QuestAction.SetVariable("var1", contract.requiredKills())), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			finalKill.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(contract.endNpcId(),
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START, Map.of("var0", 0, "var1", 0), definition);
		List<Integer> targetNpcIds = contract.targetNpcIds().stream().toList();
		for (int count = 1; count <= contract.requiredKills(); count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(targetNpcIds.get((count - 1) % targetNpcIds.size())));
			snapshot = nextSnapshot(snapshot, plan);
			if (count < contract.requiredKills()) {
				assertEquals(QuestStatus.START, snapshot.status());
				assertEquals(Map.of("var0", 0, "var1", count),
					definition.progressLayout().unpack(snapshot.packedVariables()));
				assertNoMatch(compiled, snapshot, reportEvent);
			} else {
				assertEquals(QuestStatus.REWARD, snapshot.status());
				assertEquals(Map.of("var0", 1, "var1", contract.requiredKills()),
					definition.progressLayout().unpack(snapshot.packedVariables()));
			}
		}
		assertNoMatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.targetNpcIds().iterator().next()));

		QuestTransition reopen = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.endNpcId(), QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reopen.afterCommit());
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.endNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(contract.endNpcId(), QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, contract.exp(), QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", contract.firstRewardItemId(), contract.firstRewardItemAmount(),
				QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		if (contract.startNpcId() != contract.endNpcId()) {
			assertTrue(routes(definition, "unaccepted", contract.endNpcId()).isEmpty());
			assertTrue(routes(definition, "reward", contract.startNpcId()).isEmpty());
		}
		assertEquals(contract.startNpcId() == contract.endNpcId()
				? Set.of(contract.startNpcId())
				: Set.of(contract.startNpcId(), contract.endNpcId()),
			definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
				.collect(Collectors.toUnmodifiableSet()));
		assertEquals(contract.requiredKills() == 1 ? 1 : 2, definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet)
			.count());
		assertTrue(definition.transitions().stream().allMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc
				|| transition.event() instanceof QuestEvent.KillNpcSet));
	}

	private static void assertLegacyReportedMonsterHunt(CompiledQuestDefinition compiled,
			ReportedMonsterHuntContract contract) {
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, Map.of("var0", contract.requiredKills()));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", contract.requiredKills()));

		QuestEvent targets = new QuestEvent.KillNpcSet(contract.targetNpcIds());
		QuestTransition finalKill;
		if (contract.requiredKills() == 1) {
			finalKill = transition(definition, "started", "ready", targets);
			assertEquals(1, finalKill.priority());
			assertEquals(List.of(new QuestCondition.VariableBelow("var0", 1)), finalKill.conditions());
		} else {
			QuestTransition continuing = transition(definition, "started", "started", targets);
			assertEquals(1, continuing.priority());
			assertEquals(List.of(new QuestCondition.VariableBelow("var0", contract.requiredKills() - 1)),
				continuing.conditions());
			assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), continuing.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				continuing.afterCommit());
			finalKill = transition(definition, "started", "ready", targets);
			assertEquals(0, finalKill.priority());
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", contract.requiredKills() - 1),
					new QuestCondition.VariableBelow("var0", contract.requiredKills())),
				finalKill.conditions());
		}
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			finalKill.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(contract.endNpcId(),
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START, Map.of("var0", 0), definition);
		List<Integer> targetNpcIds = contract.targetNpcIds().stream().toList();
		for (int count = 1; count <= contract.requiredKills(); count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(targetNpcIds.get((count - 1) % targetNpcIds.size())));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
			if (count < contract.requiredKills()) {
				assertNoMatch(compiled, snapshot, reportEvent);
			}
		}
		assertNoMatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.targetNpcIds().iterator().next()));

		QuestTransition reportPage = transition(definition, "ready", "ready",
			new QuestEvent.TalkToNpc(contract.endNpcId(), QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportPage.afterCommit());
		QuestTransition report = transition(definition, "ready", "reward", reportEvent);
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, reportEvent));
		assertEquals(QuestStatus.REWARD, snapshot.status());
		assertEquals(Map.of("var0", contract.requiredKills()),
			definition.progressLayout().unpack(snapshot.packedVariables()));

		QuestTransition reopen = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.endNpcId(), QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reopen.afterCommit());
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.endNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(contract.endNpcId(), QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, contract.exp(), QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", contract.firstRewardItemId(), contract.firstRewardItemAmount(),
				QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		if (contract.startNpcId() != contract.endNpcId()) {
			assertTrue(routes(definition, "unaccepted", contract.endNpcId()).isEmpty());
			assertTrue(routes(definition, "ready", contract.startNpcId()).isEmpty());
			assertTrue(routes(definition, "reward", contract.startNpcId()).isEmpty());
		}
		assertEquals(contract.startNpcId() == contract.endNpcId()
				? Set.of(contract.startNpcId())
				: Set.of(contract.startNpcId(), contract.endNpcId()),
			definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
				.collect(Collectors.toUnmodifiableSet()));
		assertEquals(contract.requiredKills() == 1 ? 1 : 2, definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet)
			.count());
		assertTrue(definition.transitions().stream().allMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc
				|| transition.event() instanceof QuestEvent.KillNpcSet));
	}

	/**
	 * 顺序 SECTION 链总判据（生产视图定义）：链式前缀节点逐段推进；跨段目标集互斥且前段进行中
	 * 后段目标不计数；未满节点 QUEST_SELECT 显示客户端成功页、1009 带全满门禁；满节点 1009 无门禁
	 * 进领奖；完成流发放真端奖励并收尾 CompleteQuest。
	 * The sequential SECTION-chain contract (production-view definitions): chained prefix nodes
	 * advance stage by stage; cross-stage target sets are disjoint and later stages never count
	 * early; unfinished nodes show the client success page with a fully-gated 1009 recovery, the
	 * full node's 1009 enters reward ungated, and completion grants the retail rewards.
	 */
	private static void assertSequentialSectionChain(SequentialChainContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		List<Integer> counts = contract.stageCounts();
		int stages = counts.size();
		List<List<Integer>> chain = chainedPrefixStates(counts);
		List<String> labels = chain.stream().map(
			QuestLegacyMonsterHuntProductionFlowTest::chainLabel).toList();
		Map<String, Integer> zero = chainProjection(chain.get(0));
		Map<String, Integer> full = chainProjection(chain.get(chain.size() - 1));
		assertNode(definition, "unaccepted", QuestStatus.NONE, zero);
		for (int index = 0; index < chain.size(); index++) {
			final int state = index;
			assertNode(definition, labels.get(state), QuestStatus.START,
				chainProjection(chain.get(state)));
		}
		assertNode(definition, "reward", QuestStatus.REWARD, full);
		assertNode(definition, "complete", QuestStatus.COMPLETE, zero);

		// 链上击杀边：每条边从当前链节点推进到下一节点（PACKET_ONLY）；真端目标集按段覆盖。
		// 段号 = 链态上首个未满段（前缀态里同一段占多个链态）。
		// Kill edges on the chain: every edge advances the current node to the next one
		// (PACKET_ONLY); per-stage retail targets must be covered. A chain state's stage is its
		// first unfinished slot (one stage spans several chain states).
		List<Set<Integer>> perStateTargets = new ArrayList<>();
		List<Integer> stateStages = new ArrayList<>();
		for (int index = 0; index < chain.size() - 1; index++) {
			final String source = labels.get(index);
			List<QuestTransition> edges = definition.transitions().stream()
				.filter(candidate -> source.equals(candidate.sourceNode()))
				.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc)
				.toList();
			assertTrue(!edges.isEmpty(), () -> source + " 必须携带当前段击杀边");
			Set<Integer> targets = new java.util.TreeSet<>();
			for (QuestTransition edge : edges) {
				assertEquals(labels.get(index + 1), edge.targetNode(), edge::toString);
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					edge.afterCommit());
				assertTrue(edge.event() instanceof QuestEvent.KillNpc killNpc
					&& targets.add(killNpc.npcId()));
			}
			int stage = firstUnfinishedSlot(chain.get(index), counts);
			assertTrue(stage >= 0 && stage < stages, () -> source + " 段号越界");
			if (stage < contract.stageTargets().size()) {
				assertTrue(targets.containsAll(contract.stageTargets().get(stage)),
					() -> source + " 击杀集必须覆盖真端目标，实际 " + targets);
			}
			perStateTargets.add(targets);
			stateStages.add(stage);
		}
		// 段间互斥：同一目标不跨段计数；前段进行中后段样本不产生任何击杀计划（乱序不计数）。
		// Stage isolation: targets never count across stages; a later stage's sample produces no
		// kill plan while an earlier stage is in progress (out-of-order kills never count).
		List<Set<Integer>> perStageTargets = new ArrayList<>();
		for (int stage = 0; stage < stages; stage++) {
			Set<Integer> merged = new java.util.TreeSet<>();
			for (int index = 0; index < perStateTargets.size(); index++) {
				if (stateStages.get(index) == stage) {
					merged.addAll(perStateTargets.get(index));
				}
			}
			if (stage > 0) {
				assertTrue(java.util.Collections.disjoint(merged, perStageTargets.get(stage - 1)),
					() -> "相邻段目标集必须互斥");
			}
			perStageTargets.add(merged);
		}
		for (int npcId : contract.excludedNpcIds()) {
			assertNoMatch(compiled, snapshot(contract.questId(), QuestStatus.START, zero, definition),
				new QuestEvent.KillNpc(npcId));
		}
		for (int later = 1; later < perStageTargets.size(); later++) {
			final int sample = perStageTargets.get(later).iterator().next();
			assertNoMatch(compiled,
				snapshot(contract.questId(), QuestStatus.START, zero, definition),
				new QuestEvent.KillNpc(sample));
		}

		// 顺序推进模拟：逐状态击杀当前段样本，投影精确落到下一链节点。
		// Sequential walk: killing the current stage's sample advances the projection exactly to
		// the next chain node.
		QuestSnapshot current = snapshot(contract.questId(), QuestStatus.START, zero, definition);
		for (int index = 0; index < chain.size() - 1; index++) {
			final int state = index;
			current = nextSnapshot(current, dispatch(compiled, current,
				new QuestEvent.KillNpc(perStateTargets.get(state).iterator().next())));
			assertEquals(QuestStatus.START, current.status());
			assertEquals(chainProjection(chain.get(state + 1)),
				definition.progressLayout().unpack(current.packedVariables()));
		}

		// 接取流：ACCEPT_SIMPLE 带 StartEligible 落在链首节点。
		// Accept flow: ACCEPT_SIMPLE lands on the first chain node with StartEligible.
		QuestTransition accept = transition(definition, "unaccepted", labels.get(0),
			new QuestEvent.TalkToNpc(contract.acquireNpcId(), QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());

		// 未满链节点：无 QUEST_SELECT/1009 报告通道（P0-2 顺序链规范形，页链不再由服务端驱动，
		// 提前上交不可达；FINISH_DIALOG 关窗出口保留）。
		// Unfinished chain nodes: no QUEST_SELECT/1009 report channel (canonical sequential shape
		// since P0-2; pages are no longer server-driven, early turn-in is unreachable; the
		// FINISH_DIALOG close exit stays).
		String fullLabel = labels.get(labels.size() - 1);
		for (int index = 0; index < chain.size() - 1; index++) {
			final String label = labels.get(index);
			assertTrue(definition.transitions().stream().noneMatch(candidate ->
				label.equals(candidate.sourceNode())
					&& candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == contract.reportNpcId() && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> label + " 不得保留报告通道路由");
		}

		// 满链节点：QUEST_SELECT 无门禁直翻领奖并按档位查表下发奖励窗（1009 中转删除）。
		// Full chain node: the QUEST_SELECT flips reward ungated with the tiered window (no 1009 hop).
		QuestTransition report = transition(definition, fullLabel, "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
			report.afterCommit());

		// 完成流：SELECTED_QUEST_REWARD1 发放真端奖励并收尾 CompleteQuest。
		// Completion: SELECTED_QUEST_REWARD1 grants the retail rewards and ends in CompleteQuest.
		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		List<QuestAction> expectedCompletionActions = new ArrayList<>(contract.rewards());
		expectedCompletionActions.add(new QuestAction.CompleteQuest(0));
		assertEquals(expectedCompletionActions, completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		// 完成模拟：REWARD 态确认后 COMPLETE 且计数清零。
		// Completion simulation: the confirm route moves REWARD to COMPLETE with counters reset.
		QuestSnapshot reward = snapshot(contract.questId(), QuestStatus.REWARD, full, definition);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, reward, completion.event(), completion)
			.orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(zero, definition.progressLayout().unpack(plan.nextPackedVariables()));
	}

	/** 链式前缀状态：每步推进首个未满段。 / Chained prefix states; each step fills the first unfinished stage. */
	private static List<List<Integer>> chainedPrefixStates(List<Integer> counts) {
		List<List<Integer>> chain = new ArrayList<>();
		chain.add(new ArrayList<>(java.util.Collections.nCopies(counts.size(), 0)));
		while (true) {
			List<Integer> current = chain.get(chain.size() - 1);
			List<Integer> next = new ArrayList<>(current);
			boolean advanced = false;
			for (int slot = 0; slot < counts.size(); slot++) {
				if (next.get(slot) < counts.get(slot)) {
					next.set(slot, next.get(slot) + 1);
					advanced = true;
					break;
				}
			}
			if (!advanced) {
				return List.copyOf(chain);
			}
			chain.add(next);
		}
	}

	/** 链态上首个未满段；满态返回 -1。 / The state's first unfinished slot; -1 on the full state. */
	private static int firstUnfinishedSlot(List<Integer> state, List<Integer> counts) {
		for (int slot = 0; slot < counts.size(); slot++) {
			if (state.get(slot) < counts.get(slot)) {
				return slot;
			}
		}
		return -1;
	}

	/** 网格段标签（编译器 {@code label()} 同构）：a0b0…。 / Grid segment label mirroring the compiler. */
	private static String chainLabel(List<Integer> state) {
		StringBuilder label = new StringBuilder();
		for (int index = 0; index < state.size(); index++) {
			label.append((char) ('a' + index)).append(state.get(index));
		}
		return label.toString();
	}

	/** 段计数 → 变量投影。 / Stage counts to the variable projection. */
	private static Map<String, Integer> chainProjection(List<Integer> state) {
		Map<String, Integer> values = new java.util.LinkedHashMap<>();
		for (int index = 0; index < state.size(); index++) {
			values.put("var" + index, state.get(index));
		}
		return java.util.Map.copyOf(values);
	}

	/**
	 * 顺序 SECTION 链契约数据：接取/报告 NPC、各段所需击杀、真端目标集（可空 = 从定义边推导）、
	 * 排除样本与真端奖励。
	 * Holds a sequential SECTION-chain contract: acquire/report NPCs, per-stage required kills,
	 * retail targets (empty = derive from the definition edges), excluded samples, and rewards.
	 */
	private record SequentialChainContract(int questId, int acquireNpcId, int reportNpcId,
			List<Integer> stageCounts, List<Set<Integer>> stageTargets, Set<Integer> excludedNpcIds,
			List<QuestAction.GrantReward> rewards) {

		SequentialChainContract(int questId, int acquireNpcId, int reportNpcId, List<Integer> stageCounts,
				List<Set<Integer>> stageTargets, List<QuestAction.GrantReward> rewards) {
			this(questId, acquireNpcId, reportNpcId, stageCounts, stageTargets, Set.of(), rewards);
		}
	}

	private static void assertSimpleMonsterHunt(MonsterHuntContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		if (isGridShaped(definition)) {
			assertGridSimpleMonsterHunt(compiled, contract);
			return;
		}
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", contract.requiredKills()));

		QuestEvent configuredEvent = new QuestEvent.KillNpcSet(contract.targetNpcIds());
		QuestTransition continuing = transition(definition, "started", "started", configuredEvent);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", contract.requiredKills() - 1)),
			continuing.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 0), new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "started", "reward", configuredEvent);
		assertEquals(0, completion.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", contract.requiredKills() - 1)),
			completion.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1), new QuestAction.SetVariable("var1", contract.requiredKills())),
			completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			completion.afterCommit());

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		for (int count = 1; count < contract.requiredKills(); count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", 0, "var1", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}

		QuestMutationPlan finalKill = dispatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
		assertEquals(QuestStatus.REWARD, finalKill.nextStatus());
		assertEquals(Map.of("var0", 1, "var1", contract.requiredKills()),
			definition.progressLayout().unpack(finalKill.nextPackedVariables()));
		assertEquals(completion.actions(), finalKill.requiredActions());
	}

	/**
	 * 网格形单段击杀合同（P0-2 规范形）：击杀步进无条件/动作（计数即节点），饱和段仍是 START，
	 * 满段 QUEST_SELECT 直接翻 REWARD 并按档位查表下发奖励窗；未满段无 QUEST_SELECT/1009 报告通道。
	 * Single-section grid kill contract (canonical since P0-2): kill steps are unconditional, the
	 * saturated segment stays START, the full node's QUEST_SELECT flips REWARD with the tiered reward
	 * window, and incomplete segments keep no QUEST_SELECT/1009 report channel.
	 */
	private static void assertGridSimpleMonsterHunt(CompiledQuestDefinition compiled,
			MonsterHuntContract contract) throws Exception {
		QuestDefinition definition = compiled.definition();
		int kills = contract.requiredKills();
		QuestNode zero = gridNode(definition, 0);
		QuestNode full = gridNode(definition, kills);
		QuestNode reward = nodeByProjection(definition, QuestStatus.REWARD, Map.of("var0", kills));

		QuestEvent kill = new QuestEvent.KillNpc(contract.sampleTargetNpcId());
		QuestTransition continuing = transition(definition, zero.label(), gridNode(definition, 1).label(), kill);
		assertEquals(List.of(), continuing.conditions());
		assertEquals(List.of(), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		// 合同未携带交付 NPC，从满段 QUEST_SELECT 交付边提取（P0-2 规范形，交付即翻领奖）。
		// The contract carries no delivery NPC; extract it from the full-node QUEST_SELECT delivery
		// edge (canonical since P0-2, delivering flips reward directly).
		int reportNpc = definition.transitions().stream()
			.filter(candidate -> full.label().equals(candidate.sourceNode())
				&& reward.label().equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
			.mapToInt(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).npcId())
			.findFirst().orElseThrow();
		QuestTransition deliver = transition(definition, full.label(), reward.label(),
			new QuestEvent.TalkToNpc(reportNpc, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
			deliver.afterCommit());

		// 未满段：无 QUEST_SELECT/1009 报告通道（页链不再由服务端驱动），提前上交不可达。
		// Incomplete segment: no QUEST_SELECT/1009 report channel (pages are no longer
		// server-driven), so early turn-in is unreachable.
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			zero.label().equals(candidate.sourceNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
			() -> "quest " + contract.questId() + " 未满段不得保留报告通道路由");
		QuestEvent earlyReport = new QuestEvent.TalkToNpc(reportNpc, QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertNoMatch(compiled, snapshot(contract.questId(), QuestStatus.START, Map.of("var0", 0), definition),
			earlyReport);

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= kills; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, kill));
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		assertNoMatch(compiled, snapshot, kill);
		// 满段交付进领奖。 / Delivering from the saturated segment enters reward.
		QuestMutationPlan plan = dispatch(compiled, snapshot, deliver.event());
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
	}

	/**
	 * 网格形报告击杀合同（DD hunt 行，含带简报行；P0-2 规范形）：击杀步进无条件、满段 QUEST_SELECT
	 * 交付进领奖（按档位查表下发奖励窗）；未满段提前上交不可达；REWARD 态无 QUEST_SELECT 重开
	 * （P0c-8c transXmlOnly 判例）；领奖/完成尾部与旧形同构。
	 * Grid reported-hunt contract (with or without briefing; canonical since P0-2): unconditional
	 * kill steps, the saturated segment's QUEST_SELECT delivers into reward with the tiered window,
	 * early turn-in is impossible, and the reward state keeps no QUEST_SELECT re-open.
	 */
	private static void assertGridReportedMonsterHunt(CompiledQuestDefinition compiled,
			ReportedMonsterHuntContract contract) throws Exception {
		QuestDefinition definition = compiled.definition();
		int kills = contract.requiredKills();
		QuestNode reward = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.REWARD)
			.findFirst().orElseThrow();
		Map<String, Integer> fullKillVars = new java.util.LinkedHashMap<>(reward.projection().variables());
		fullKillVars.remove("var5");
		Map<String, Integer> zeroKillVars = new java.util.LinkedHashMap<>(fullKillVars);
		zeroKillVars.put("var0", 0);
		QuestNode full = nodeByKillProjection(definition, QuestStatus.START, fullKillVars);
		QuestNode zero = nodeByKillProjection(definition, QuestStatus.START, zeroKillVars);
		int reportNpc = contract.endNpcId();

		// 满段 QUEST_SELECT 交付路由存在（P0-2 规范形），按档位查表翻 REWARD。
		// The saturated-segment QUEST_SELECT delivery exists (canonical since P0-2) and flips
		// reward with the tiered window.
		QuestEvent reportEvent = new QuestEvent.TalkToNpc(reportNpc, QuestDialogAction.QUEST_SELECT.id());
		QuestTransition report = transition(definition, full.label(), reward.label(), reportEvent);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
			report.afterCommit());
		// 未满段（零段）无提前上交可达路由。
		// No early turn-in route is reachable from the zero segment.
		assertNoMatch(compiled,
			snapshot(contract.questId(), QuestStatus.START, zero.projection().variables(), definition),
			reportEvent);

		// 击杀轮转推进网格（任一目标都为该段计数），满段仍是 START，报告才进领奖。
		// Kills rotate the grid forward; only the report moves on to reward.
		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			zero.projection().variables(), definition);
		List<Integer> targetNpcIds = contract.targetNpcIds().stream().toList();
		for (int count = 1; count <= kills; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(targetNpcIds.get((count - 1) % targetNpcIds.size())));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			Map<String, Integer> expected = new java.util.LinkedHashMap<>(full.projection().variables());
			expected.put("var0", count);
			assertEquals(expected, definition.progressLayout().unpack(snapshot.packedVariables()));
			if (count < kills) {
				assertNoMatch(compiled, snapshot, reportEvent);
			}
		}
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(targetNpcIds.getFirst()));
		QuestMutationPlan reportPlan = dispatch(compiled, snapshot, reportEvent);
		assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());
		assertEquals(full.projection().variables(),
			definition.progressLayout().unpack(reportPlan.nextPackedVariables()));

		// 真端在 REWARD 态无 QUEST_SELECT 重开路由（P0c-8c transXmlOnly 判例）；1009/USE_OBJECT
		// 预览再开同一窗（完成流保留）。
		// Retail keeps no reward-state QUEST_SELECT re-open (P0c-8c transXmlOnly); the 1009 and
		// USE_OBJECT previews from the completion flow re-open the same window.
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			reward.label().equals(candidate.sourceNode()) && reward.label().equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			() -> "retail " + contract.questId() + " must not keep the legacy reward-state re-open route");
		QuestTransition preview = transition(definition, reward.label(), reward.label(),
			new QuestEvent.TalkToNpc(reportNpc, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = transition(definition, reward.label(), "complete",
			new QuestEvent.TalkToNpc(reportNpc, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, contract.exp(), QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", contract.firstRewardItemId(), contract.firstRewardItemAmount(),
				QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertQuest1842Routes(QuestDefinition definition) {
		QuestEvent regular = new QuestEvent.KillNpcSet(QUEST_1842_REGULAR_TARGETS);
		assertCounterRoute(transition(definition, "started", "started", regular, 2), 2,
			List.of(new QuestCondition.VariableBelow("var0", 79)), "var0");
		assertCounterRoute(transition(definition, "started", "started", regular, 1), 1,
			List.of(
				new QuestCondition.QuestVariableIs("var0", 79),
				new QuestCondition.VariableBelow("var1", 1)), "var0");
		assertCounterRoute(transition(definition, "started", "ready", regular), 0,
			List.of(
				new QuestCondition.QuestVariableIs("var0", 79),
				new QuestCondition.VariableAtLeast("var1", 1)), "var0");

		QuestEvent general = new QuestEvent.KillNpc(215134);
		assertCounterRoute(transition(definition, "started", "started", general), 1,
			List.of(
				new QuestCondition.VariableBelow("var1", 1),
				new QuestCondition.VariableBelow("var0", 80)), "var1");
		assertCounterRoute(transition(definition, "started", "ready", general), 0,
			List.of(
				new QuestCondition.VariableBelow("var1", 1),
				new QuestCondition.VariableAtLeast("var0", 80)), "var1");
	}

	private static void assertCounterRoute(QuestTransition transition, int priority,
			List<QuestCondition> conditions, String field) {
		assertEquals(priority, transition.priority());
		assertEquals(conditions, transition.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), transition.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
	}

	private static void assertQuest1842Order(CompiledQuestDefinition compiled, boolean generalFirst) {
		QuestDefinition definition = compiled.definition();
		QuestSnapshot snapshot = snapshot(1842, QuestStatus.START, Map.of("var0", 0, "var1", 0), definition);
		if (generalFirst) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(215134)));
			assertEquals(Map.of("var0", 0, "var1", 1), definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		for (int count = 1; count <= 80; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(215094)));
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(count, definition.progressLayout().unpack(snapshot.packedVariables()).get("var0"));
		}
		if (!generalFirst) {
			assertEquals(Map.of("var0", 80, "var1", 0),
				definition.progressLayout().unpack(snapshot.packedVariables()));
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(215134)));
		}
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(Map.of("var0", 80, "var1", 1), definition.progressLayout().unpack(snapshot.packedVariables()));
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(215094));
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(215134));
		QuestMutationPlan report = dispatch(compiled, snapshot,
			new QuestEvent.TalkToNpc(278503, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", 80, "var1", 1),
			definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	private static void assertReport(QuestDefinition definition, String source, int npcId) {
		QuestTransition page = transition(definition, source, source,
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), page.conditions());
		assertEquals(List.of(), page.actions());
		assertNull(page.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			page.afterCommit());

		QuestTransition report = transition(definition, source, "reward",
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertNull(report.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static void assertNoMatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot, QuestEvent event) {
		assertTrue(compiled.definition().transitions().stream().noneMatch(transition ->
			QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent()));
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status, Map<String, Integer> variables,
			QuestDefinition definition) {
		return new QuestSnapshot(7, questId, status, definition.progressLayout().pack(variables), Map.of());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	/**
	 * 网格形判据：无 started 节点（真端击杀网格行的接取直落网格零段，DD hunt 行形状）。
	 * Grid-shape discriminator: no started node — the accept lands directly on the zero segment.
	 */
	private static boolean isGridShaped(QuestDefinition definition) {
		return definition.nodes().stream().noneMatch(node -> "started".equals(node.label()));
	}

	/**
	 * (状态, 变量投影) 寻址（P0c-8a 判例）：网格段标签随段数变化，合同改按投影定位节点。
	 * Projection-based node addressing: grid segment labels vary with the section count.
	 */
	private static QuestNode nodeByProjection(QuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		return definition.nodes().stream()
			.filter(candidate -> candidate.projection().status() == status
				&& candidate.projection().variables().equals(variables))
			.findFirst().orElseThrow();
	}

	/** 单段网格节点（a&lt;kills&gt;）：var0 = 击杀数。 / Single-section grid node, var0 = kills. */
	private static QuestNode gridNode(QuestDefinition definition, int kills) {
		return nodeByProjection(definition, QuestStatus.START, Map.of("var0", kills));
	}

	/**
	 * 按「击杀投影」寻址：忽略 var5 简报标志位后比较变量投影——带简报网格行的 REWARD 投影
	 * 不带标志位而网格节点带（家族编译器形状），逐段定位必须对标志位免疫。
	 * Kill-projection addressing: compare variables ignoring the var5 briefing flag — briefing grid
	 * rows carry the flag on grid nodes but not on the reward node (family compiler shape).
	 */
	private static QuestNode nodeByKillProjection(QuestDefinition definition, QuestStatus status,
			Map<String, Integer> killVars) {
		return definition.nodes().stream()
			.filter(candidate -> candidate.projection().status() == status)
			.filter(candidate -> {
				Map<String, Integer> trimmed = new java.util.LinkedHashMap<>(candidate.projection().variables());
				trimmed.remove("var5");
				return trimmed.equals(killVars);
			})
			.findFirst().orElseThrow();
	}

	/**
	 * START 节点按打包值升序；真端击杀网格的末位即"饱和段"（也是报告段），次末位是最后一杀的来源。
	 * START nodes in ascending packed order: the grid's last entry is the saturated/report segment.
	 */
	private static List<QuestNode> startStepsByPack(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START)
			.sorted(Comparator.comparingInt(node ->
				definition.progressLayout().pack(node.projection().variables())))
			.toList();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return transition(definition, source, target, event, null);
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, Integer priority) {
		List<QuestTransition> matches = definition.transitions().stream()
			// 生产定义带无 source 的 enter-world 自愈边（QE-046），按 source 过滤时必须容忍 null。
			// Production definitions carry source-less enter-world recovery edges, so null sources are skipped.
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source))
			.filter(candidate -> Objects.equals(candidate.targetNode(), target))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> priority == null || Objects.equals(candidate.priority(), priority))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event + " " + priority);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}

	/**
	 * 保存简单 monster-hunt 的目标和阈值差异。
	 * Holds target and threshold differences for simple monster hunts.
	 */
	private record MonsterHuntContract(int questId, Set<Integer> targetNpcIds, int sampleTargetNpcId,
			int requiredKills) {
	}

	/**
	 * 保存需要结束 NPC 报告的 monster-hunt 目标、owner 和奖励差异。
	 * Holds target, owner, and reward differences for monster hunts reported to an end NPC.
	 */
	private record ReportedMonsterHuntContract(int questId, Set<Integer> targetNpcIds, int startNpcId,
			int endNpcId, int requiredKills, long exp, int firstRewardItemId, long firstRewardItemAmount) {

		private ReportedMonsterHuntContract(int questId, Set<Integer> targetNpcIds, int startNpcId,
				int endNpcId, long exp, int firstRewardItemId) {
			this(questId, targetNpcIds, startNpcId, endNpcId, 10, exp, firstRewardItemId, 1);
		}

		private ReportedMonsterHuntContract(int questId, Set<Integer> targetNpcIds, int startNpcId,
				int endNpcId, int requiredKills, long exp, int firstRewardItemId) {
			this(questId, targetNpcIds, startNpcId, endNpcId, requiredKills, exp, firstRewardItemId, 1);
		}
	}
}
