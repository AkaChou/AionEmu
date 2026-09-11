package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for routes filled from the complete origin/history template index. */
class LegacyTemplateMirrorRouteRegressionTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");

	private record ItemMirror(int questId, int npcId, int actionId, int failurePageId) {
	}

	private record DialogRoute(int questId, String source, int npcId, int actionId, String targetNode,
			List<AfterCommitAction> afterCommit) {
	}

	@Test
	void itemCollectingMirrorsUseTheClientOwnedReportAndTurnInProtocol() throws Exception {
		for (ItemMirror mirror : List.of(
			new ItemMirror(2237, 700145, 20002, 0),
			new ItemMirror(2527, 700328, 39, 2716),
			new ItemMirror(3096, 700423, 39, 2716),
			new ItemMirror(3096, 700424, 39, 2716),
			new ItemMirror(3096, 700425, 39, 2716),
			new ItemMirror(3096, 700426, 39, 2716),
			new ItemMirror(11003, 798933, 39, 2716),
			new ItemMirror(80356, 831815, 20002, 2716),
			new ItemMirror(80365, 831827, 20002, 2716))) {
			QuestDefinition definition = compile(mirror.questId());
			assertPage(definition, "started", mirror.npcId(), 2375);

			List<QuestTransition> checks = talkRoutes(
				definition, "started", mirror.npcId(), mirror.actionId());
			assertEquals(2, checks.size(), "quest " + mirror.questId() + " item checks");
			QuestTransition success = checks.stream()
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.findFirst().orElseThrow();
			QuestTransition failure = checks.stream()
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.findFirst().orElseThrow();
			List<QuestCondition> expectedConditions = definition.metadata().itemRequirements().stream()
				.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count(), true))
				.toList();
			List<QuestAction> expectedActions = definition.metadata().itemRequirements().stream()
				.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
				.toList();

			assertEquals("reward", success.targetNode(), "quest " + mirror.questId() + " success target");
			assertEquals(expectedConditions, success.conditions(), "quest " + mirror.questId() + " conditions");
			assertEquals(expectedActions, success.actions(), "quest " + mirror.questId() + " removals");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(5)), success.afterCommit(),
				"quest " + mirror.questId() + " success response");
			assertEquals("started", failure.targetNode(), "quest " + mirror.questId() + " failure target");
			assertTrue(failure.conditions().isEmpty(), "quest " + mirror.questId() + " failure conditions");
			assertTrue(failure.actions().isEmpty(), "quest " + mirror.questId() + " failure actions");
			assertEquals(mirror.failurePageId() == 0
					? List.of(new AfterCommitAction.CloseDialog())
					: List.of(new AfterCommitAction.ShowQuestDialog(mirror.failurePageId())),
				failure.afterCommit(), "quest " + mirror.questId() + " failure response");
		}
	}

	@Test
	void eventShardStartsUseTheClientOwnedSelectNonePage() throws Exception {
		for (int questId : List.of(50031, 50038, 50040, 50041)) {
			QuestDefinition definition = compile(questId);
			QuestTransition start = definition.transitions().stream()
				.filter(transition -> transition.sourceNode().equals("unaccepted"))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(31).equals(talk.dialogId()))
				.findFirst().orElseThrow();
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4762)), start.afterCommit(),
				"quest " + questId + " start page");
		}
	}

	@Test
	void quest25602UsesTheClientSuccessReportAndRewardResponse() throws Exception {
		QuestDefinition definition = compile(25602);
		assertPage(definition, "s3", 806171, 10002);
		QuestTransition reward = talkRoutes(definition, "s3", 806171, 1009).getFirst();
		assertEquals("reward", reward.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(5)), reward.afterCommit());
	}

	@Test
	void uniqueClientGraphCandidatesUseOwnedAcceptAndReportPages() throws Exception {
		for (int questId : List.of(50089, 50090)) {
			QuestDefinition definition = compile(questId);
			for (int npcId : List.of(835680, 835681)) {
				List<QuestTransition> accepts = talkRoutes(definition, "unaccepted", npcId, 1002);
				assertEquals(1, accepts.size(), "quest " + questId + " accept route count");
				QuestTransition accept = accepts.getFirst();
				assertEquals("started", accept.targetNode(), "quest " + questId + " accept target");
				assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions(),
					"quest " + questId + " accept conditions");
				assertTrue(accept.actions().isEmpty(), "quest " + questId + " accept actions");
				assertEquals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(1011)), accept.afterCommit(),
					"quest " + questId + " accept response");
			}
		}

		QuestDefinition windstream = compile(21080);
		for (int npcId : List.of(799231, 799427)) {
			assertPage(windstream, "started", npcId, 10002);
			List<QuestTransition> reward = talkRoutes(windstream, "started", npcId, 1009);
			QuestTransition success = reward.stream()
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.findFirst().orElseThrow();
			QuestTransition failure = reward.stream()
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(5)), success.afterCommit(),
				"quest 21080 success response for " + npcId);
			assertEquals(List.of(new AfterCommitAction.CloseDialog()), failure.afterCommit(),
				"quest 21080 failure response for " + npcId);
		}
	}

	@Test
	void quest2920RewardChoicesUseTheHandlerProvenRewardWindows() throws Exception {
		QuestDefinition definition = compile(2920);
		for (int[] choice : List.of(new int[] {10010, 5}, new int[] {10011, 6})) {
			List<QuestTransition> routes = talkRoutes(definition, "started", 204141, choice[0]);
			assertEquals(1, routes.size(), "quest 2920 choice " + choice[0]);
			QuestTransition route = routes.getFirst();
			assertEquals("reward", route.targetNode(), "quest 2920 choice target " + choice[0]);
			assertTrue(route.conditions().isEmpty(), "quest 2920 choice conditions " + choice[0]);
			assertTrue(route.actions().isEmpty(), "quest 2920 choice actions " + choice[0]);
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(choice[1])), route.afterCommit(),
				"quest 2920 choice response " + choice[0]);
		}
	}

	@Test
	void handlerProvenFollowUpDialogsUseTheirPageTargetState() throws Exception {
		for (DialogRoute expected : List.of(
			new DialogRoute(1345, "reward", 204006, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1535, "started", 204580, 10000, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(5))),
			new DialogRoute(1535, "started", 204580, 10001, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(6))),
			new DialogRoute(1535, "started", 204580, 10002, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(7))),
			new DialogRoute(1640, "unaccepted", 730033, 10000, "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(1640, "started", 730033, 10001, "complete",
				List.of(new AfterCommitAction.RefreshPlayerStats(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(1722, "s2", 278544, 10002, "s2",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2002, "s10", 790002, 10003, "s11",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(2230, "started", 203621, 10000, "started",
				List.of(new AfterCommitAction.StartQuestTimer(1800),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(1687, "started", 204601, 10009, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(1687, "started", 204601, 10019, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(1687, "started", 204601, 10029, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(2004, "v2", 203539, 10001, "v2",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2284, "step1", 798040, 10001, "step1",
				List.of(new AfterCommitAction.DeleteInteractionNpc(true),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog())),
			new DialogRoute(2303, "started", 798082, 10009, "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.ShowQuestDialog(1012))),
			new DialogRoute(2303, "started", 798082, 10019, "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.ShowQuestDialog(1097))),
			new DialogRoute(2332, "started", 798084, 10000, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(5))),
			new DialogRoute(2332, "started", 798084, 10001, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(6))),
			new DialogRoute(2332, "started", 798084, 10002, "reward",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(7))),
			new DialogRoute(2333, "reward", 798084, 10001, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(3205, "s16", 804601, 10001, "s16",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(14112, "reward", 203195, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(14153, "reward", 204505, 10001, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(15672, "unaccepted", 806114, 1009, "unaccepted",
				List.of(new AfterCommitAction.ShowQuestDialog(5))),
			new DialogRoute(20032, "started", 799239, 10001, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(20032, "started", 799258, 10001, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(20032, "started", 799503, 10001, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(25672, "unaccepted", 806116, 1009, "unaccepted",
				List.of(new AfterCommitAction.ShowQuestDialog(5))),
			new DialogRoute(27510, "unaccepted", 806079, 10000, "unaccepted",
				List.of(new AfterCommitAction.CloseDialog())))) {
			QuestDefinition definition = compile(expected.questId());
			List<QuestTransition> routes = talkRoutes(definition, expected.source(), expected.npcId(),
				expected.actionId());
			assertEquals(1, routes.size(), "quest " + expected.questId() + " action " + expected.actionId());
			QuestTransition route = routes.getFirst();
			assertEquals(expected.targetNode(), route.targetNode(),
				"quest " + expected.questId() + " action " + expected.actionId() + " target");
			assertEquals(expected.afterCommit(), route.afterCommit(),
				"quest " + expected.questId() + " action " + expected.actionId() + " response");
		}

		QuestDefinition teleporter = compile(1640);
		QuestTransition completedDialog = talkRoutes(teleporter, "complete", 730033, 10000).getFirst();
		assertEquals("complete", completedDialog.targetNode());
		assertTrue(completedDialog.afterCommit().contains(new AfterCommitAction.CloseDialog()));
	}

	@Test
	void handlerProvenItemChoicesRetainTheirItemChecksAndRemovalActions() throws Exception {
		QuestDefinition definition = compile(1535);
		for (int[] choice : List.of(
			new int[] {10000, 182201818, 5},
			new int[] {10001, 182201819, 3},
			new int[] {10002, 182201820, 1})) {
			QuestTransition route = talkRoutes(definition, "started", 204580, choice[0]).getFirst();
			assertEquals(List.of(new QuestCondition.HasItem(choice[1], choice[2], true)), route.conditions());
			assertEquals(List.of(
				new QuestAction.RemoveItem(182201818, -1),
				new QuestAction.RemoveItem(182201819, -1),
				new QuestAction.RemoveItem(182201820, -1)), route.actions());
		}
	}

	@Test
	void quest24154UsesTheExistingV1ItemPresentationRoute() throws Exception {
		QuestDefinition definition = compile(24154);
		assertTrue(talkRoutes(definition, "started", 204809, 31).isEmpty());
		assertTrue(talkRoutes(definition, "started", 204809, 1353).isEmpty());
		List<QuestTransition> routes = talkRoutes(definition, "v1", 204809, 1353);
		assertEquals(1, routes.size());
		assertEquals(List.of(
			new QuestAction.GiveItem(182215463, 1),
			new QuestAction.GiveItem(185000006, 1)), routes.getFirst().actions());
	}

	@Test
	void legacyTemplateCloseControlsDoNotChangeQuestState() throws Exception {
		for (DialogRoute expected : List.of(
			new DialogRoute(1115, "unaccepted", 203072, 10000, "unaccepted",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1131, "shugo", 799093, 10000, "shugo",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1309, "reward", 203830, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1323, "reward", 203939, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(1423, "started", 203983, 10000, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2107, "k1", 203516, 10000, "k1",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2321, "reward", 790018, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2435, "reward", 204390, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2578, "reward", 204746, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(2670, "reward", 204208, 10000, "reward",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(26820, "s1", 806233, 10000, "s1",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(35025, "started", 798972, 10000, "started",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(50010, "unaccepted", 202549, 10001, "unaccepted",
				List.of(new AfterCommitAction.CloseDialog())),
			new DialogRoute(13950, "unaccepted", 806075, 20000, "started",
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog())))) {
			QuestDefinition definition = compile(expected.questId());
			List<QuestTransition> routes = talkRoutes(definition, expected.source(), expected.npcId(),
				expected.actionId());
			assertEquals(1, routes.size(), "quest " + expected.questId() + " action " + expected.actionId());
			assertEquals(expected.targetNode(), routes.getFirst().targetNode(),
				"quest " + expected.questId() + " target");
			assertEquals(expected.afterCommit(), routes.getFirst().afterCommit(),
				"quest " + expected.questId() + " response");
		}
	}

	@Test
	void laterItemReportNpcsShowTheTurnInPageAndBoundStartItemsAreGranted() throws Exception {
		for (int[] route : List.of(
			new int[] {16976, 801762},
			new int[] {26976, 801764},
			new int[] {16985, 804864},
			new int[] {26985, 804866})) {
			QuestDefinition definition = compile(route[0]);
			assertPage(definition, "s1", route[1], 1352);
		}

		for (int[] start : List.of(
			new int[] {18395, 798926, 182216153},
			new int[] {28395, 799225, 182216155})) {
			QuestDefinition definition = compile(start[0]);
			QuestTransition route = talkRoutes(definition, "unaccepted", start[1], 10000).getFirst();
			assertEquals("started", route.targetNode());
			assertEquals(List.of(new QuestAction.GiveItem(start[2], 1)), route.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), route.afterCommit());
		}
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId, int pageId) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, 31);
		assertEquals(1, routes.size(), "quest " + definition.id() + " page route count");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), routes.getFirst().afterCommit(),
			"quest " + definition.id() + " page response");
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId,
			int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
	}
}
