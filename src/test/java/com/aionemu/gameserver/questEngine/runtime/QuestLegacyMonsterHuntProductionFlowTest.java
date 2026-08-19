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
		new ReportedMonsterHuntContract(30708, Set.of(800425, 800426, 800427),
			800369, 800438, 5, 7086913, 186000201),
		new ReportedMonsterHuntContract(30758, Set.of(800425, 800426, 800427),
			800369, 800438, 5, 7086913, 186000201));

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
		QuestTransition continuing = transition(definition, "started", "started", targets);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", contract.requiredKills() - 1)),
			continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());
		QuestTransition finalKill = transition(definition, "started", "ready", targets);
		assertEquals(0, finalKill.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", contract.requiredKills() - 1)),
			finalKill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), finalKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			finalKill.afterCommit());

		QuestEvent reportEvent = new QuestEvent.TalkToNpc(contract.endNpcId(),
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START, Map.of("var0", 0), definition);
		for (int count = 1; count <= contract.requiredKills(); count++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.targetNpcIds().iterator().next()));
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
			new QuestAction.GrantReward("ITEM", contract.firstRewardItemId(), 1,
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
		assertEquals(2, definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet)
			.count());
		assertTrue(definition.transitions().stream().allMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc
				|| transition.event() instanceof QuestEvent.KillNpcSet));
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
	 * 保存需要结束 NPC 报告的 monster-hunt 目标、owner 和奖励差异。
	 * Holds target, owner, and reward differences for monster hunts reported to an end NPC.
	 */
	private record ReportedMonsterHuntContract(int questId, Set<Integer> targetNpcIds, int startNpcId,
			int endNpcId, int requiredKills, long exp, int firstRewardItemId) {

		private ReportedMonsterHuntContract(int questId, Set<Integer> targetNpcIds, int startNpcId,
				int endNpcId, long exp, int firstRewardItemId) {
			this(questId, targetNpcIds, startNpcId, endNpcId, 10, exp, firstRewardItemId);
		}
	}
}
