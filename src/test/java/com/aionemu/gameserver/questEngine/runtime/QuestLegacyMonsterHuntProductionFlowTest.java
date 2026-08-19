package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
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
	private static final List<IndependentReportedMonsterHuntContract> INDEPENDENT_REPORTED_HUNTS = List.of(
		new IndependentReportedMonsterHuntContract(30514, 211),
		new IndependentReportedMonsterHuntContract(30564, 222));
	private static final List<IndependentCountedMonsterHuntContract> INDEPENDENT_COUNTED_HUNTS = List.of(
		new IndependentCountedMonsterHuntContract(25406, 883644, 883645),
		new IndependentCountedMonsterHuntContract(25407, 883646, 883648),
		new IndependentCountedMonsterHuntContract(25408, 883909, 883911));
	private static final List<IndependentGroupedCountedMonsterHuntContract> INDEPENDENT_GROUPED_COUNTED_HUNTS = List.of(
		new IndependentGroupedCountedMonsterHuntContract(25580,
			Set.of(241246, 241480, 241484, 241488),
			Set.of(241247, 241481, 241485, 241489)));
	private static final List<List<Integer>> INDEPENDENT_TRIPLE_KILL_ORDERS = List.of(
		List.of(217185, 217195, 217204),
		List.of(217185, 217204, 217195),
		List.of(217195, 217185, 217204),
		List.of(217195, 217204, 217185),
		List.of(217204, 217185, 217195),
		List.of(217204, 217195, 217185));

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
	Stream<DynamicTest> independentReportedMonsterHuntsAcceptEitherRetailKillOrder() {
		return INDEPENDENT_REPORTED_HUNTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertIndependentReportedMonsterHunt(contract)));
	}

	@TestFactory
	Stream<DynamicTest> independentCountedMonsterHuntsAcceptEitherRetailKillOrder() {
		return INDEPENDENT_COUNTED_HUNTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertIndependentCountedMonsterHunt(contract)));
	}

	@TestFactory
	Stream<DynamicTest> independentGroupedCountedMonsterHuntsAcceptEitherRetailKillOrder() {
		return INDEPENDENT_GROUPED_COUNTED_HUNTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertIndependentGroupedCountedMonsterHunt(contract)));
	}

	@Test
	void quest27541RequiresThreeIndependentKillsBeforeTheRetailReport() throws Exception {
		assertIndependentTripleCountedMonsterHunt(27541, 799558, List.of(
			new QuestAction.GrantReward("EXP", 0, 2814541, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 162000050, 10, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 164000070, 10, QuestRewardAmountMode.EXACT)));
	}

	@Test
	void quest17541RequiresThreeIndependentKillsBeforeTheRetailReport() throws Exception {
		assertIndependentTripleCountedMonsterHunt(17541, 799553, List.of(
			new QuestAction.GrantReward("EXP", 0, 2814541, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("AP", 0, 450, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 162000050, 10, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 164000070, 10, QuestRewardAmountMode.EXACT)));
	}

	@Test
	void quest25060UsesTheClientSuccessPageWhileObjectivesAreIncomplete() throws Exception {
		QuestDefinition definition = load(25060).definition();
		QuestTransition progressPage = transition(definition, "started", "started",
			new QuestEvent.TalkToNpc(804730, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			progressPage.afterCommit());
		QuestTransition rewardPage = transition(definition, "started", "reward",
			new QuestEvent.TalkToNpc(804730, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var1", 1),
			new QuestCondition.QuestVariableIs("var2", 1),
			new QuestCondition.QuestVariableIs("var3", 1)), rewardPage.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			rewardPage.afterCommit());
	}

	@Test
	void quests25090And25093UseTheClientSuccessPageWhileObjectivesAreIncomplete() throws Exception {
		for (int questId : List.of(25090, 25093)) {
			QuestDefinition definition = load(questId).definition();
			QuestTransition progressPage = transition(definition, "started", "started",
				new QuestEvent.TalkToNpc(804928, QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				progressPage.afterCommit());
		}
	}

	@Test
	void quests25002Through25203UseTheClientSuccessPageWhileObjectivesAreIncomplete() throws Exception {
		for (Map.Entry<Integer, Integer> entry : Map.of(
			25002, 804903,
			25010, 804721,
			25201, 804914,
			25202, 804914,
			25203, 804914,
			25325, 805343).entrySet()) {
			QuestDefinition definition = load(entry.getKey()).definition();
			QuestTransition progressPage = transition(definition, "started", "started",
				new QuestEvent.TalkToNpc(entry.getValue(), QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				progressPage.afterCommit());
		}
	}

	@Test
	void challengeMonsterHuntsUseTheRetailClientAcceptAndReportPages() throws Exception {
		for (int questId : List.of(27160, 27161)) {
			QuestDefinition definition = load(questId).definition();
			Set<Integer> targets = questId == 27160
				? Set.of(219700, 219777, 219788)
				: Set.of(235832, 235914, 235918);
			assertEquals(new QuestEvent.KillNpcSet(targets),
				transition(definition, "started", "started", new QuestEvent.KillNpcSet(targets), 1).event());
			assertEquals(new QuestEvent.KillNpcSet(targets),
				transition(definition, "started", "ready", new QuestEvent.KillNpcSet(targets), 0).event());
			QuestTransition accept = transition(definition, "unaccepted", "started",
				new QuestEvent.TalkToNpc(804719, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
			assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), accept.afterCommit());
			QuestTransition reportPage = transition(definition, "ready", "ready",
				new QuestEvent.TalkToNpc(804719, QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				reportPage.afterCommit());
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
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "m1", QuestStatus.START, Map.of("var0", 10));
		QuestEvent configuredEvent = new QuestEvent.KillNpcSet(Set.of(216102, 216103));
		assertCounterRoute(transition(definition, "started", "started", configuredEvent), 1,
			List.of(new QuestCondition.VariableBelow("var0", 9)), "var0");
		assertCounterRoute(transition(definition, "started", "m1", configuredEvent), 0,
			List.of(new QuestCondition.QuestVariableIs("var0", 9)), "var0");

		QuestEvent event = new QuestEvent.KillNpc(216102);
		QuestSnapshot snapshot = snapshot(21120, QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= 10; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, event);
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count), definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		assertNoMatch(compiled, snapshot, event);
		assertReport(definition, "m1", 799291);
		QuestMutationPlan report = dispatch(compiled, snapshot,
			new QuestEvent.TalkToNpc(799291, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", 10), definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	@Test
	void quest26988CountsFiveKillsBeforeRetailReportAndRewardsAt804867() throws Exception {
		CompiledQuestDefinition compiled = load(26988);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "k1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 5));

		QuestEvent targets = new QuestEvent.KillNpcSet(Set.of(233126, 233127, 233128));
		QuestTransition continuing = transition(definition, "started", "started", targets);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableBelow("var1", 4)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition finalKill = transition(definition, "started", "k1", targets);
		assertEquals(0, finalKill.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", 4)), finalKill.conditions());
		assertEquals(List.of(
			new QuestAction.IncrementVariable("var1", 1),
			new QuestAction.SetVariable("var0", 1)), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			finalKill.afterCommit());

		QuestSnapshot snapshot = snapshot(26988, QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		for (int count = 1; count <= 5; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(233126));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count == 5 ? 1 : 0, "var1", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(233126));

		QuestTransition reportPage = transition(definition, "k1", "k1",
			new QuestEvent.TalkToNpc(804867, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), reportPage.conditions());
		assertEquals(List.of(), reportPage.actions());
		assertNull(reportPage.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportPage.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(804867,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestTransition report = transition(definition, "k1", "reward", reportEvent);
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertNull(report.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		QuestMutationPlan reportPlan = dispatch(compiled, snapshot, reportEvent);
		snapshot = nextSnapshot(snapshot, reportPlan);
		assertEquals(QuestStatus.REWARD, snapshot.status());
		assertEquals(Map.of("var0", 1, "var1", 5),
			definition.progressLayout().unpack(snapshot.packedVariables()));

		QuestTransition reopen = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(804867, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reopen.afterCommit());
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
		assertNode(definition, "ready", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));

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
		QuestTransition continuing = transition(definition, "started", "started", targets);
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 2)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition finalKill = transition(definition, "started", "ready", targets);
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", 2)), finalKill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			finalKill.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(806700,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(29691, QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= 3; count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(246200));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
			if (count < 3) {
				assertNoMatch(compiled, snapshot, reportEvent);
			}
		}
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(246200));

		QuestTransition reportPage = transition(definition, "ready", "ready",
			new QuestEvent.TalkToNpc(806700, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportPage.afterCommit());
		QuestTransition report = transition(definition, "ready", "reward", reportEvent);
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		QuestMutationPlan reportPlan = dispatch(compiled, snapshot, reportEvent);
		snapshot = nextSnapshot(snapshot, reportPlan);
		assertEquals(QuestStatus.REWARD, snapshot.status());
		assertEquals(Map.of("var0", 3), definition.progressLayout().unpack(snapshot.packedVariables()));

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
	void quest30715UsesTheClientSelect5ReportAfterTheRetailKill() throws Exception {
		CompiledQuestDefinition compiled = load(30715);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));

		QuestEvent configuredTarget = new QuestEvent.KillNpcSet(Set.of(219357));
		QuestEvent target = new QuestEvent.KillNpc(219357);
		QuestTransition kill = transition(definition, "started", "ready", configuredTarget);
		assertEquals(1, kill.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 1)), kill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), kill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			kill.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(804870,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(30715, QuestStatus.START, Map.of("var0", 0), definition);
		assertNoMatch(compiled, snapshot, reportEvent);
		snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, target));
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(Map.of("var0", 1), definition.progressLayout().unpack(snapshot.packedVariables()));
		assertNoMatch(compiled, snapshot, target);

		QuestTransition reportPage = transition(definition, "ready", "ready",
			new QuestEvent.TalkToNpc(804870, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
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

		QuestTransition reopen = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(804870, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
			reopen.afterCommit());
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
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", contract.requiredKills() - 1)),
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
		assertEquals(Set.of(contract.startNpcId(), contract.endNpcId()),
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

	private static void assertIndependentReportedMonsterHunt(
			IndependentReportedMonsterHuntContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		Map<String, Integer> incomplete = Map.of("var0", 0, "var1", 0);
		Map<String, Integer> ready = Map.of("var0", 1, "var1", 1);
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, ready);
		assertNode(definition, "reward", QuestStatus.REWARD, ready);
		assertNode(definition, "complete", QuestStatus.COMPLETE, ready);

		assertIndependentKillRoutes(definition, 217310, "var0", "var1");
		assertIndependentKillRoutes(definition, 217317, "var1", "var0");
		assertIndependentKillOrder(compiled, 217310, 217317);
		assertIndependentKillOrder(compiled, 217317, 217310);

		QuestTransition accept = transition(definition, "unaccepted", "started",
			new QuestEvent.TalkToNpc(799592, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertNull(accept.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(799670,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestTransition reportPage = transition(definition, "ready", "ready",
			new QuestEvent.TalkToNpc(799670, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), reportPage.conditions());
		assertEquals(List.of(), reportPage.actions());
		assertNull(reportPage.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportPage.afterCommit());
		QuestTransition report = transition(definition, "ready", "reward", reportEvent);
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertNull(report.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestTransition reopen = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(799670, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reopen.afterCommit());
		QuestTransition preview = transition(definition, "reward", "reward", reportEvent);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(799670, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 598320, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 5097837, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("TITLE", contract.titleId(), 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		assertTrue(routes(definition, "unaccepted", 799670).isEmpty());
		assertTrue(routes(definition, "ready", 799592).isEmpty());
		assertTrue(routes(definition, "reward", 799592).isEmpty());
		assertEquals(Set.of(799592, 799670), definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(Collectors.toUnmodifiableSet()));
		assertEquals(Set.of(217310, 217317), definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc)
			.map(transition -> ((QuestEvent.KillNpc) transition.event()).npcId())
			.collect(Collectors.toUnmodifiableSet()));
		assertEquals(4, definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc)
			.count());
		assertTrue(definition.transitions().stream().allMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc
				|| transition.event() instanceof QuestEvent.KillNpc));
		assertEquals(incomplete, definition.progressLayout().unpack(
			definition.progressLayout().pack(incomplete)));
	}

	private static void assertIndependentCountedMonsterHunt(
			IndependentCountedMonsterHuntContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		Map<String, Integer> ready = Map.of("var0", 4, "var1", 4);
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, ready);
		assertNode(definition, "reward", QuestStatus.REWARD, ready);
		assertNode(definition, "complete", QuestStatus.COMPLETE, ready);
		assertEquals(Set.of("unaccepted", "started", "ready", "reward", "complete"),
			definition.nodes().stream().map(node -> node.label()).collect(Collectors.toUnmodifiableSet()));

		assertIndependentCountedKillRoutes(definition, contract.firstTargetId(), "var0", "var1");
		assertIndependentCountedKillRoutes(definition, contract.secondTargetId(), "var1", "var0");
		assertIndependentCountedKillOrder(compiled, contract, contract.firstTargetId(), contract.secondTargetId());
		assertIndependentCountedKillOrder(compiled, contract, contract.secondTargetId(), contract.firstTargetId());

		QuestTransition accept = transition(definition, "unaccepted", "started",
			new QuestEvent.TalkToNpc(805401, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertNull(accept.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(805401,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestTransition reportPage = transition(definition, "ready", "ready",
			new QuestEvent.TalkToNpc(805401, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), reportPage.conditions());
		assertEquals(List.of(), reportPage.actions());
		assertNull(reportPage.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportPage.afterCommit());
		QuestTransition report = transition(definition, "ready", "reward", reportEvent);
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertNull(report.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestTransition reopen = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(805401, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reopen.afterCommit());
		QuestTransition preview = transition(definition, "reward", "reward", reportEvent);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(805401, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 53023500, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000237, 23, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		assertEquals(Set.of(805401), definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(Collectors.toUnmodifiableSet()));
		assertEquals(Set.of(contract.firstTargetId(), contract.secondTargetId()),
			definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.KillNpc)
				.map(transition -> ((QuestEvent.KillNpc) transition.event()).npcId())
				.collect(Collectors.toUnmodifiableSet()));
		assertEquals(6, definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc)
			.count());
		assertTrue(definition.transitions().stream().allMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc
				|| transition.event() instanceof QuestEvent.KillNpc));
	}

	private static void assertIndependentGroupedCountedMonsterHunt(
			IndependentGroupedCountedMonsterHuntContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		Map<String, Integer> ready = Map.of("var0", 20, "var1", 20);
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, ready);
		assertNode(definition, "reward", QuestStatus.REWARD, ready);
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));

		QuestEvent first = new QuestEvent.KillNpcSet(contract.firstTargets());
		QuestEvent second = new QuestEvent.KillNpcSet(contract.secondTargets());
		assertIndependentGroupedCountedKillRoutes(definition, first, "var0", "var1");
		assertIndependentGroupedCountedKillRoutes(definition, second, "var1", "var0");
		assertIndependentGroupedCountedKillOrder(compiled, first, second, "var0", "var1");
		assertIndependentGroupedCountedKillOrder(compiled, second, first, "var1", "var0");

		QuestTransition accept = transition(definition, "unaccepted", "started",
			new QuestEvent.TalkToNpc(806116, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());

		QuestTransition reportPage = transition(definition, "ready", "ready",
			new QuestEvent.TalkToNpc(806116, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportPage.afterCommit());
		QuestEvent reportEvent = new QuestEvent.TalkToNpc(806116,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestTransition report = transition(definition, "ready", "reward", reportEvent);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(806116, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 91465537, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000414, 2, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
		assertEquals(Set.of(806116), definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(Collectors.toUnmodifiableSet()));
		assertEquals(6, definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet)
			.count());
	}

	private static void assertIndependentGroupedCountedKillRoutes(QuestDefinition definition,
			QuestEvent event, String field, String otherField) {
		assertCounterRoute(transition(definition, "started", "started", event, 2), 2,
			List.of(new QuestCondition.VariableBelow(field, 19)), field);
		QuestTransition penultimate = transition(definition, "started", "started", event, 1);
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs(field, 19),
			new QuestCondition.VariableBelow(otherField, 20)), penultimate.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), penultimate.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			penultimate.afterCommit());
		QuestTransition finalKill = transition(definition, "started", "ready", event, 0);
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs(field, 19),
			new QuestCondition.VariableAtLeast(otherField, 20)), finalKill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			finalKill.afterCommit());
	}

	private static void assertIndependentGroupedCountedKillOrder(
			CompiledQuestDefinition compiled, QuestEvent firstEvent, QuestEvent secondEvent,
			String firstField, String secondField) {
		QuestDefinition definition = compiled.definition();
		QuestEvent reportEvent = new QuestEvent.TalkToNpc(806116,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(compiled.id(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		assertNoMatch(compiled, snapshot, reportEvent);
		int firstTarget = firstEvent instanceof QuestEvent.KillNpcSet kill
			? kill.npcIds().iterator().next() : 0;
		int secondTarget = secondEvent instanceof QuestEvent.KillNpcSet kill
			? kill.npcIds().iterator().next() : 0;
		for (int count = 1; count <= 20; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(firstTarget)));
			assertEquals(QuestStatus.START, snapshot.status());
			Map<String, Integer> values = definition.progressLayout().unpack(snapshot.packedVariables());
			assertEquals(count, values.get(firstField));
			assertEquals(0, values.get(secondField));
			assertNoMatch(compiled, snapshot, reportEvent);
		}
		for (int count = 1; count <= 20; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(secondTarget)));
			assertEquals(QuestStatus.START, snapshot.status());
			Map<String, Integer> values = definition.progressLayout().unpack(snapshot.packedVariables());
			assertEquals(20, values.get(firstField));
			assertEquals(count, values.get(secondField));
			if (count < 20) {
				assertNoMatch(compiled, snapshot, reportEvent);
			}
		}
		assertEquals(Map.of("var0", 20, "var1", 20),
			definition.progressLayout().unpack(snapshot.packedVariables()));
		QuestMutationPlan report = dispatch(compiled, snapshot, reportEvent);
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", 20, "var1", 20),
			definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	private static void assertIndependentTripleCountedMonsterHunt(int questId, int reportNpcId,
			List<QuestAction.GrantReward> rewards) throws Exception {
		CompiledQuestDefinition compiled = load(questId);
		QuestDefinition definition = compiled.definition();
		Map<String, Integer> ready = Map.of("var0", 1, "var1", 1, "var2", 1);
		Map<String, Integer> complete = Map.of("var0", 0, "var1", 0, "var2", 0);
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "ready", QuestStatus.START, ready);
		assertNode(definition, "reward", QuestStatus.REWARD, ready);
		assertNode(definition, "complete", QuestStatus.COMPLETE, complete);

		assertIndependentTripleCountedKillRoutes(definition, new QuestEvent.KillNpc(217185),
			"var0", "var1", "var2");
		assertIndependentTripleCountedKillRoutes(definition, new QuestEvent.KillNpc(217195),
			"var1", "var0", "var2");
		QuestEvent thirdTargets = new QuestEvent.KillNpcSet(Set.of(217204, 217206));
		assertIndependentTripleCountedKillRoutes(definition, thirdTargets, "var2", "var0", "var1");

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(reportNpcId,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		for (List<Integer> order : INDEPENDENT_TRIPLE_KILL_ORDERS) {
			QuestSnapshot snapshot = snapshot(questId, QuestStatus.START,
				Map.of("var0", 0, "var1", 0, "var2", 0), definition);
			assertNoMatch(compiled, snapshot, reportEvent);
			for (int index = 0; index < order.size(); index++) {
				int target = order.get(index);
				snapshot = nextSnapshot(snapshot,
					dispatch(compiled, snapshot, new QuestEvent.KillNpc(target)));
				assertEquals(QuestStatus.START, snapshot.status());
				Map<String, Integer> values = definition.progressLayout().unpack(snapshot.packedVariables());
				assertEquals(order.subList(0, index + 1).contains(217185) ? 1 : 0,
					values.get("var0"));
				assertEquals(order.subList(0, index + 1).contains(217195) ? 1 : 0,
					values.get("var1"));
				assertEquals(order.subList(0, index + 1).contains(217204) ? 1 : 0,
					values.get("var2"));
				if (index < order.size() - 1) {
					assertNoMatch(compiled, snapshot, reportEvent);
				}
			}
			assertEquals(ready, definition.progressLayout().unpack(snapshot.packedVariables()));
			QuestMutationPlan report = dispatch(compiled, snapshot, reportEvent);
			assertEquals(QuestStatus.REWARD, report.nextStatus());
			assertEquals(ready, definition.progressLayout().unpack(report.nextPackedVariables()));
		}

		QuestSnapshot alternate = snapshot(questId, QuestStatus.START,
			Map.of("var0", 0, "var1", 0, "var2", 0), definition);
		for (int target : List.of(217185, 217195, 217206)) {
			alternate = nextSnapshot(alternate,
				dispatch(compiled, alternate, new QuestEvent.KillNpc(target)));
		}
		assertEquals(ready, definition.progressLayout().unpack(alternate.packedVariables()));

		for (int target : List.of(246160, 246161, 246196, 217205, 246261)) {
			assertNoMatch(compiled, snapshot(questId, QuestStatus.START,
				Map.of("var0", 0, "var1", 0, "var2", 0), definition),
				new QuestEvent.KillNpc(target));
		}

		QuestTransition accept = transition(definition, "unaccepted", "started",
			new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()));
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());
		QuestTransition reportPage = transition(definition, "ready", "ready",
			new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			reportPage.afterCommit());
		QuestTransition report = transition(definition, "ready", "reward", reportEvent);
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		QuestTransition completion = transition(definition, "reward", "complete",
			new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.SELECTED_QUEST_REWARD1.id()));
		List<QuestAction> expectedCompletionActions = new java.util.ArrayList<>(rewards);
		expectedCompletionActions.add(new QuestAction.CompleteQuest(0));
		assertEquals(expectedCompletionActions, completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
		assertEquals(12, definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc
				|| transition.event() instanceof QuestEvent.KillNpcSet)
			.count());
		assertEquals(Set.of(reportNpcId), definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.collect(Collectors.toUnmodifiableSet()));
	}

	private static void assertIndependentTripleCountedKillRoutes(QuestDefinition definition,
			QuestEvent event, String field, String firstOtherField, String secondOtherField) {
		assertTripleCountedRoute(definition, event, field, "started", 3, List.of(
			new QuestCondition.VariableBelow(field, 1),
			new QuestCondition.VariableBelow(firstOtherField, 1),
			new QuestCondition.VariableBelow(secondOtherField, 1)));
		assertTripleCountedRoute(definition, event, field, "started", 2, List.of(
			new QuestCondition.VariableBelow(field, 1),
			new QuestCondition.VariableAtLeast(firstOtherField, 1),
			new QuestCondition.VariableBelow(secondOtherField, 1)));
		assertTripleCountedRoute(definition, event, field, "started", 1, List.of(
			new QuestCondition.VariableBelow(field, 1),
			new QuestCondition.VariableBelow(firstOtherField, 1),
			new QuestCondition.VariableAtLeast(secondOtherField, 1)));
		QuestTransition finalKill = transition(definition, "started", "ready", event, 0);
		assertEquals(List.of(
			new QuestCondition.VariableBelow(field, 1),
			new QuestCondition.VariableAtLeast(firstOtherField, 1),
			new QuestCondition.VariableAtLeast(secondOtherField, 1)), finalKill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			finalKill.afterCommit());
	}

	private static void assertTripleCountedRoute(QuestDefinition definition, QuestEvent event,
			String field, String target, int priority, List<QuestCondition> conditions) {
		QuestTransition route = transition(definition, "started", target, event, priority);
		assertEquals(conditions, route.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable(field, 1)), route.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			route.afterCommit());
	}

	private static void assertIndependentCountedKillRoutes(QuestDefinition definition, int npcId,
			String field, String otherField) {
		QuestEvent event = new QuestEvent.KillNpc(npcId);
		assertCounterRoute(transition(definition, "started", "started", event, 2), 2,
			List.of(new QuestCondition.VariableBelow(field, 3)), field);
		assertCounterRoute(transition(definition, "started", "started", event, 1), 1,
			List.of(
				new QuestCondition.QuestVariableIs(field, 3),
				new QuestCondition.VariableBelow(otherField, 4)), field);
		assertCounterRoute(transition(definition, "started", "ready", event), 0,
			List.of(
				new QuestCondition.QuestVariableIs(field, 3),
				new QuestCondition.VariableAtLeast(otherField, 4)), field);
	}

	private static void assertIndependentCountedKillOrder(CompiledQuestDefinition compiled,
			IndependentCountedMonsterHuntContract contract, int firstNpcId, int secondNpcId) {
		QuestDefinition definition = compiled.definition();
		QuestEvent reportEvent = new QuestEvent.TalkToNpc(805401,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(compiled.id(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		assertNoMatch(compiled, snapshot, reportEvent);
		for (int count = 1; count <= 4; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(firstNpcId)));
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(firstNpcId == contract.firstTargetId() ? Map.of("var0", count, "var1", 0)
				: Map.of("var0", 0, "var1", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(firstNpcId));
		assertNoMatch(compiled, snapshot, reportEvent);
		for (int count = 1; count <= 4; count++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(secondNpcId)));
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(secondNpcId == contract.firstTargetId() ? Map.of("var0", count, "var1", 4)
				: Map.of("var0", 4, "var1", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
			if (count < 4) {
				assertNoMatch(compiled, snapshot, reportEvent);
			}
		}
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(contract.firstTargetId()));
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(contract.secondTargetId()));
		QuestMutationPlan report = dispatch(compiled, snapshot, reportEvent);
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", 4, "var1", 4),
			definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	private static void assertIndependentKillRoutes(QuestDefinition definition, int npcId,
			String field, String otherField) {
		QuestEvent event = new QuestEvent.KillNpc(npcId);
		assertCounterRoute(transition(definition, "started", "started", event), 1,
			List.of(
				new QuestCondition.VariableBelow(field, 1),
				new QuestCondition.VariableBelow(otherField, 1)), field);
		assertCounterRoute(transition(definition, "started", "ready", event), 0,
			List.of(
				new QuestCondition.VariableBelow(field, 1),
				new QuestCondition.VariableAtLeast(otherField, 1)), field);
	}

	private static void assertIndependentKillOrder(CompiledQuestDefinition compiled,
			int firstNpcId, int secondNpcId) {
		QuestDefinition definition = compiled.definition();
		QuestEvent reportEvent = new QuestEvent.TalkToNpc(799670,
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(compiled.id(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		assertNoMatch(compiled, snapshot, reportEvent);
		snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(firstNpcId)));
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(firstNpcId == 217310 ? Map.of("var0", 1, "var1", 0)
			: Map.of("var0", 0, "var1", 1), definition.progressLayout().unpack(snapshot.packedVariables()));
		assertNoMatch(compiled, snapshot, reportEvent);
		snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(secondNpcId)));
		assertEquals(QuestStatus.START, snapshot.status());
		assertEquals(Map.of("var0", 1, "var1", 1),
			definition.progressLayout().unpack(snapshot.packedVariables()));
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(217310));
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(217317));
		QuestMutationPlan report = dispatch(compiled, snapshot, reportEvent);
		assertEquals(QuestStatus.REWARD, report.nextStatus());
		assertEquals(Map.of("var0", 1, "var1", 1),
			definition.progressLayout().unpack(report.nextPackedVariables()));
	}

	private static void assertSimpleMonsterHunt(MonsterHuntContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", contract.requiredKills()));

		QuestEvent configuredEvent = new QuestEvent.KillNpcSet(contract.targetNpcIds());
		QuestTransition continuing = transition(definition, "started", "started", configuredEvent);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", contract.requiredKills() - 1)),
			continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "started", "reward", configuredEvent);
		assertEquals(0, completion.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", contract.requiredKills() - 1)),
			completion.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", contract.requiredKills())),
			completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			completion.afterCommit());

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var0", 0), definition);
		for (int count = 1; count < contract.requiredKills(); count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", count),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}

		QuestMutationPlan finalKill = dispatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
		assertEquals(QuestStatus.REWARD, finalKill.nextStatus());
		assertEquals(Map.of("var0", contract.requiredKills()),
			definition.progressLayout().unpack(finalKill.nextPackedVariables()));
		assertEquals(completion.actions(), finalKill.requiredActions());
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

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return transition(definition, source, target, event, null);
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, Integer priority) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> priority == null || Objects.equals(candidate.priority(), priority))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event + " " + priority);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestLegacyMonsterHuntProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存简单 monster-hunt 的目标和阈值差异。
	 * Holds target and threshold differences for simple monster hunts.
	 */
	private record MonsterHuntContract(int questId, Set<Integer> targetNpcIds, int sampleTargetNpcId,
			int requiredKills) {
	}

	/**
	 * 保存两个独立单杀目标与任务标题奖励的差异。
	 * Holds title-reward differences for hunts with two independent single-kill objectives.
	 */
	private record IndependentReportedMonsterHuntContract(int questId, int titleId) {
	}

	/**
	 * 保存两个独立四杀目标的任务和 NPC 差异。
	 * Holds quest and NPC differences for hunts with two independent four-kill objectives.
	 */
	private record IndependentCountedMonsterHuntContract(int questId, int firstTargetId, int secondTargetId) {
	}

	/**
	 * 保存两个独立多目标计数组的任务和 NPC 差异。
	 * Holds quest and NPC differences for two independent multi-target counters.
	 */
	private record IndependentGroupedCountedMonsterHuntContract(int questId, Set<Integer> firstTargets,
			Set<Integer> secondTargets) {
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
