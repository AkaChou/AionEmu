package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.e2e.HandoverContinuationContract;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCollectingDialogProtocolAlignmentTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");
	private static final int[] ITEM_COLLECTING_QUESTS = {
		13968, 15011, 15021, 15022, 15044, 15052, 15071, 15102, 15103, 15230, 15231,
		15232, 15307, 15323, 15403, 15404, 15405, 15502, 15505, 15508, 15511, 15514,
		15517, 15523, 15526, 15532, 15535, 15538, 15540, 15541, 15665, 15666, 15689,
		15691, 18742, 18975, 18976, 18977, 18978, 23968, 25012, 25020, 25033, 25085,
		25091, 25092, 25307, 25323, 25403, 25404, 25405, 25502, 25505, 25508, 25511,
		25517, 25523, 25540, 25541, 25665, 25666, 25689, 25691, 28742, 28975, 28976,
		28977, 28978, 29010, 29016, 29022, 29028, 29034, 50052,
		50053, 50054, 50055, 50056, 50057, 50088, 50089, 50090, 50094, 80723, 80724,
		80725, 80726, 80727, 80728, 80729, 80730, 80735, 80736, 80834, 80835, 80836, 80837,
		80838, 80839, 80840, 80841, 80870, 80871, 80872, 80874, 80877, 80878, 80881,
		80900, 80901, 80902, 80903, 80904, 80905, 80906, 80907,
		80908, 80909, 80910, 80911, 80912, 80913, 80914, 80915, 80916, 80917, 80918,
		80919, 80947, 80948, 80949, 80950, 80951, 80953
	};
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");
	private static ClientResourceOracle clientOracle;

	private static final List<Integer> DROP_SOURCE_QUESTS = List.of(
		15011, 15021, 15022, 15044, 15052, 15071, 15102, 15103, 15502, 15505, 15508,
		15511, 15514, 15517, 15523, 15526, 15532, 15535, 15538, 25502, 25505, 25508,
		25511, 25517, 25523);

	@Test
	void retailItemCollectingQuestsUseTheClientItemCheckProtocol() throws Exception {
		for (int questId : ITEM_COLLECTING_QUESTS) {
			QuestDefinition definition = compile(questId);
			List<Integer> startNpcs = startNpcs(questId);
			List<Integer> endNpcs = endNpcs(questId);

			assertEquals(startNpcs, dialogNpcs(definition, "unaccepted", 31),
				"quest " + questId + " start NPCs");
			for (int npcId : startNpcs) {
				assertDialogPage(definition, "unaccepted", npcId, 31, 4762);
			}

			assertEquals(endNpcs, dialogNpcs(definition, "started", 39),
				"quest " + questId + " turn-in NPCs");
			for (int npcId : endNpcs) {
				assertDialogPage(definition, "started", npcId, 31, 1011);
				assertDialogPage(definition, "reward", npcId, 31, 10002);
				assertItemCheck(definition, npcId);
			}

			Set<Integer> collectedItemIds = definition.metadata().itemRequirements().stream()
				.map(QuestItemRequirement::itemId).collect(java.util.stream.Collectors.toSet());
			definition.transitions().stream()
				.filter(transition -> "reward".equals(transition.sourceNode()))
				.forEach(transition -> {
					assertFalse(transition.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.HasItem item
						&& collectedItemIds.contains(item.itemId())), "quest " + questId + " reward item condition");
					assertFalse(transition.actions().stream().anyMatch(action -> action instanceof QuestAction.RemoveItem item
						&& collectedItemIds.contains(item.itemId())), "quest " + questId + " repeated item removal");
				});
			assertTrue(definition.transitions().stream().anyMatch(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())), "quest " + questId + " completion route");
		}
	}

	@Test
	void lunaItemTurnInsKeepStartAndEndNpcOwnershipSeparate() throws Exception {
		for (int questId : List.of(80870, 80871, 80872, 80874)) {
			QuestDefinition definition = compile(questId);
			assertTrue(talkRoutes(definition, "started", 833825, 31).isEmpty(),
				"quest " + questId + " start NPC must not own started selection");
			assertTrue(talkRoutes(definition, "started", 833825, 39).isEmpty(),
				"quest " + questId + " start NPC must not own item check");
			assertTrue(talkRoutes(definition, "reward", 833825).isEmpty(),
				"quest " + questId + " start NPC must not own reward routes");
		}
	}

	@Test
	void collectedItemDropSourcesDoNotOwnQuestDialogs() throws Exception {
		for (int questId : DROP_SOURCE_QUESTS) {
			QuestDefinition definition = compile(questId);
			Set<Integer> dropSources = definition.metadata().drops().stream()
				.map(QuestDrop::npcId).collect(java.util.stream.Collectors.toSet());
			assertFalse(dropSources.isEmpty(), "quest " + questId + " drop sources");
			assertFalse(definition.transitions().stream()
				.anyMatch(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& dropSources.contains(talk.npcId())),
				"quest " + questId + " drop source must not own dialog routes");
		}
	}

	@Test
	void craftingMasterQuestsGrantTheirDesignOnAcceptance() throws Exception {
		for (MasterItemQuest quest : List.of(
			new MasterItemQuest(29010, 204104, 152232012),
			new MasterItemQuest(29016, 204106, 152232013),
			new MasterItemQuest(29022, 204110, 152232014),
			new MasterItemQuest(29028, 204108, 152232016),
			new MasterItemQuest(29034, 204102, 152232015))) {
			QuestDefinition definition = compile(quest.id());
			List<QuestTransition> accepts = talkRoutes(definition, "unaccepted", quest.npcId(), 20000);
			assertEquals(1, accepts.size(), "quest " + quest.id() + " acceptance route");
			assertTrue(accepts.getFirst().actions().contains(new QuestAction.GiveItem(quest.designItemId(), 1)),
				"quest " + quest.id() + " design grant");
		}
	}

	@Test
	void simpleItemCheckQuestsKeepBothLegacyCheckRoutes() throws Exception {
		for (SimpleItemQuest quest : List.of(
			new SimpleItemQuest(80745, 833623), new SimpleItemQuest(80748, 833623),
			new SimpleItemQuest(80785, 833659), new SimpleItemQuest(80786, 833659),
			new SimpleItemQuest(80975, 835994), new SimpleItemQuest(80976, 835994),
			new SimpleItemQuest(80977, 835994))) {
			QuestDefinition definition = compile(quest.id());
			assertDialogPage(definition, "unaccepted", quest.npcId(), 31, 4762);
			assertDialogPage(definition, "started", quest.npcId(), 31, 1011);
			assertDialogPage(definition, "reward", quest.npcId(), 31, 10002);
			assertItemCheck(definition, quest.npcId());

			List<QuestTransition> simpleChecks = talkRoutes(definition, "started", quest.npcId(), 20002);
			assertEquals(2, simpleChecks.size(), "quest " + quest.id() + " simple check branches");
			QuestTransition success = simpleChecks.stream()
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.findFirst().orElseThrow();
			QuestTransition failure = simpleChecks.stream()
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(5)), success.afterCommit(),
				"quest " + quest.id() + " simple success");
			assertEquals(List.of(new AfterCommitAction.CloseDialog()), failure.afterCommit(),
				"quest " + quest.id() + " simple failure");
		}
	}

	@Test
	void autoStartedMisfortuneQuestsRemoveCollectedItemsOnSuccessfulCheck() throws Exception {
		for (SimpleItemQuest quest : List.of(
			new SimpleItemQuest(80945, 835303), new SimpleItemQuest(80946, 835303))) {
			QuestDefinition definition = compile(quest.id());
			assertTrue(talkRoutes(definition, "unaccepted", quest.npcId(), 31).isEmpty(),
				"quest " + quest.id() + " remains auto-started");
			assertDialogPage(definition, "started", quest.npcId(), 31, 1011);
			assertDialogPage(definition, "reward", quest.npcId(), 31, 10002);
			assertItemCheck(definition, quest.npcId());
		}
	}

	private static ClientResourceOracle oracle() throws Exception {
		if (clientOracle == null) {
			clientOracle = ClientResourceOracle.load(CLIENT_MAPPING);
		}
		return clientOracle;
	}

	private static void assertItemCheck(QuestDefinition definition, int npcId) throws Exception {
		List<QuestTransition> checks = talkRoutes(definition, "started", npcId, 39);
		assertEquals(2, checks.size(), "quest " + definition.id() + " item check branches");
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

		assertEquals("reward", success.targetNode(), "quest " + definition.id() + " successful target");
		assertEquals(expectedConditions, success.conditions(), "quest " + definition.id() + " item conditions");
		assertEquals(expectedActions, success.actions(), "quest " + definition.id() + " item removals");
		// 客户端确认页按钮是本地关闭（HACTION_FINISH_DIALOG）时不能作为续接点：同 NPC 有续接页的任务
		// 交付成功后直接下发续接页，否则保留客户端确认页 10000。
		// A client-local-close confirmation button cannot carry the continuation: quests whose same NPC has a
		// continuation page show it directly after the hand-over, everyone else keeps confirmation page 10000.
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(HandoverContinuationContract.handOverSuccessPage(definition,
				npcId, oracle()))), success.afterCommit(),
			"quest " + definition.id() + " successful page");
		assertEquals("started", failure.targetNode(), "quest " + definition.id() + " failed target");
		assertTrue(failure.conditions().isEmpty(), "quest " + definition.id() + " failed fallback");
		assertTrue(failure.actions().isEmpty(), "quest " + definition.id() + " failed actions");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10001)), failure.afterCommit(),
			"quest " + definition.id() + " failed page");
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static void assertDialogPage(QuestDefinition definition, String source, int npcId, int dialogId,
			int pageId) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, dialogId);
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " dialog " + dialogId);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), routes.getFirst().afterCommit(),
			"quest " + definition.id() + " " + source + " page");
	}

	private static List<Integer> dialogNpcs(QuestDefinition definition, String source, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.distinct().toList();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId, int dialogId) {
		return talkRoutes(definition, source, npcId).stream()
			.filter(transition -> Integer.valueOf(dialogId).equals(((QuestEvent.TalkToNpc) transition.event()).dialogId()))
			.toList();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static List<Integer> startNpcs(int questId) {
		switch (questId) {
			case 18742:
				return List.of(206378, 206379, 206380);
			case 28742:
				return List.of(206395, 206396, 206397);
			case 50052:
			case 50053:
			case 50054:
			case 50055:
			case 50056:
			case 50057:
				return List.of(833982, 833983);
			case 50088:
				return List.of(835542, 835543);
			case 50089:
			case 50090:
			case 50094:
				return List.of(835680, 835681);
			default:
				return List.of(singleStartNpc(questId));
		}
	}

	private static List<Integer> endNpcs(int questId) {
		switch (questId) {
			case 18742:
				return List.of(804707);
			case 25012:
				return List.of(804905);
			case 25085:
				return List.of(804927);
			case 25092:
				return List.of(804929);
			case 28742:
				return List.of(804732);
			case 18977:
			case 18978:
				return List.of(805215);
			case 28977:
			case 28978:
				return List.of(805218);
			case 50052:
			case 50053:
			case 50054:
			case 50055:
			case 50056:
			case 50057:
				return List.of(833982, 833983);
			case 50088:
				return List.of(835542, 835543);
			case 50089:
			case 50090:
			case 50094:
				return List.of(835680, 835681);
			case 80870:
			case 80871:
			case 80872:
			case 80874:
				return List.of(834167);
			default:
				return startNpcs(questId);
		}
	}

	private static int singleStartNpc(int questId) {
        switch (questId) {
            case 13968:
                return 835217;
            case 15011:
                return 804875;
            case 15021:
                return 804877;
            case 15022:
                return 804878;
            case 15044:
                return 804887;
            case 15052:
                return 804888;
            case 15071:
                return 804709;
            case 15102:
                return 804895;
            case 15103:
                return 804896;
            case 15230:
            case 15231:
            case 15232:
                return 805222;
            case 15307:
                return 805327;
            case 15323:
                return 805330;
            case 15403:
            case 15404:
            case 15405:
                return 805378;
            case 15502:
                return 806089;
            case 15505:
                return 806090;
            case 15508:
                return 806091;
            case 15511:
                return 806092;
            case 15514:
                return 806093;
            case 15517:
                return 806094;
            case 15523:
                return 806096;
            case 15526:
                return 806097;
            case 15532:
                return 806099;
            case 15535:
                return 806100;
            case 15538:
                return 806254;
            case 15540:
                return 806134;
            case 15541:
                return 834136;
            case 15665:
                return 806089;
            case 15666:
                return 806090;
            case 15689:
            case 15691:
                return 806696;
            case 18975:
            case 18976:
                return 805215;
            case 18977:
                return 802350;
            case 18978:
                return 802431;
            case 23968:
                return 835220;
            case 25012:
                return 804906;
            case 25020:
                return 804725;
            case 25033:
                return 804913;
            case 25085:
                return 804922;
            case 25091:
                return 804738;
            case 25092:
                return 804739;
            case 25307:
                return 805339;
            case 25323:
                return 805342;
            case 25403:
            case 25404:
            case 25405:
                return 805401;
            case 25502:
                return 806101;
            case 25505:
                return 806102;
            case 25508:
                return 806103;
            case 25511:
                return 806104;
            case 25517:
                return 806106;
            case 25523:
                return 806108;
            case 25540:
                return 806135;
            case 25541:
                return 834138;
            case 25665:
                return 806101;
            case 25666:
                return 806102;
            case 25689:
            case 25691:
                return 806697;
            case 28975:
            case 28976:
                return 805218;
            case 28977:
                return 802353;
            case 28978:
                return 802433;
            case 29010:
                return 204104;
            case 29016:
                return 204106;
            case 29022:
                return 204110;
            case 29028:
                return 204108;
            case 29034:
                return 204102;
            case 80723:
            case 80725:
            case 80727:
            case 80729:
                return 833543;
            case 80724:
            case 80726:
            case 80728:
            case 80730:
                return 833545;
            case 80735:
                return 833544;
            case 80736:
                return 833546;
            case 80834:
            case 80836:
                return 833742;
            case 80835:
            case 80837:
                return 833743;
            case 80838:
            case 80839:
            case 80840:
            case 80841:
                return 832913;
            case 80870:
            case 80871:
            case 80872:
            case 80874:
                return 833825;
            case 80877:
            case 80878:
            case 80881:
                return 834463;
            case 80900:
            case 80901:
            case 80902:
            case 80903:
            case 80904:
            case 80905:
            case 80906:
            case 80907:
            case 80908:
            case 80909:
            case 80910:
            case 80911:
            case 80912:
            case 80913:
            case 80914:
            case 80915:
            case 80916:
            case 80917:
            case 80918:
            case 80919:
                return 834418;
            case 80947:
            case 80948:
                return 835439;
            case 80949:
                return 835551;
            case 80950:
            case 80951:
                return 835552;
            case 80953:
                return 835553;
            default:
                throw new IllegalArgumentException("missing start NPC for quest " + questId);
        }
	}

	private record SimpleItemQuest(int id, int npcId) {
	}

	private record MasterItemQuest(int id, int npcId, int designItemId) {
	}
}
