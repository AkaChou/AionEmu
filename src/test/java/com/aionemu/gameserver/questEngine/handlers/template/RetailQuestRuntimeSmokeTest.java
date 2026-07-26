package com.aionemu.gameserver.questEngine.handlers.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.dataholders.StaticData;
import com.aionemu.gameserver.dataholders.XMLQuests;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.models.DataDrivenQuestData;
import com.aionemu.gameserver.questEngine.handlers.models.ItemCollectingData;
import com.aionemu.gameserver.questEngine.handlers.models.Monster;
import com.aionemu.gameserver.questEngine.handlers.models.MonsterHuntData;
import com.aionemu.gameserver.questEngine.handlers.models.NpcInfos;
import com.aionemu.gameserver.questEngine.handlers.models.ReportToData;
import com.aionemu.gameserver.questEngine.handlers.models.ReportToManyData;
import com.aionemu.gameserver.questEngine.handlers.models.XMLQuest;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;

class RetailQuestRuntimeSmokeTest {

	private static XMLQuests retailQuests;
	private static XMLQuests legacyQuests;

	@BeforeAll
	static void loadRetailQuests() throws Exception {
		Unmarshaller unmarshaller = JAXBContext.newInstance(StaticData.class).createUnmarshaller();
		retailQuests = (XMLQuests) unmarshaller.unmarshal(Path.of(
				"src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml").toFile());
		legacyQuests = (XMLQuests) unmarshaller.unmarshal(Path.of(
				"src/main/resources/aion/definitions/compact/quests/scripts/poeta.xml").toFile());
	}

	@Test
	void legacyAncientCubeRunsThroughDataDrivenQuest() {
		DataDrivenQuestData definition = definition(legacyQuests, 1127);
		assertEquals(1011, definition.getStartDialogId());
		assertEquals(List.of(798008), definition.getStartIds());
		assertEquals(List.of(798008), definition.getEndNpcIds());
		assertEquals(List.of("ACTION", "COLLECT_ITEM"), definition.getSteps().stream().map(DataDrivenQuestData.Step::getType).toList());
		assertEquals(List.of(700001), definition.getSteps().get(0).getActionIds());
		assertEquals(182200215, definition.getSteps().get(0).getGiveItemId());
		assertEquals(2375, definition.getSteps().get(1).getDialogId());

		RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
		Player player = playerWithState(1127, 0);
		assertTrue(quest.onDialogEvent(env(player, 1127, 700001, QuestDialog.NULL, -1)));
		assertEquals(1, state(player, 1127).getQuestVarById(0));
		assertEquals(182200215, quest.givenItemId);
		assertEquals(1, quest.givenItemCount);
		assertTrue(quest.onDialogEvent(env(player, 1127, 798008, QuestDialog.START_DIALOG, 31)));
		assertEquals(2375, quest.shownDialogId);
		assertTrue(quest.onDialogEvent(env(player, 1127, 798008, QuestDialog.CHECK_COLLECTED_ITEMS, 39)));
		assertTrue(quest.collectItemsChecked);
		assertEquals(QuestStatus.REWARD, state(player, 1127).getStatus());
	}

	@Test
	void compiledExpertRewardsUseRetailLeadersAndWarehouseKeepers() throws Exception {
		for (var entry : Map.of(1987, List.of(203700, 203749), 2985, List.of(204052, 204072)).entrySet()) {
			XMLQuest definition = retailQuests.getQuest().stream().filter(quest -> quest.getId() == entry.getKey()).findFirst().orElseThrow();
			assertTrue(definition instanceof ReportToData);
			assertEquals(List.of(entry.getValue().get(0)), readField(definition, "startNpcIds"));
			assertEquals(List.of(entry.getValue().get(1)), readField(definition, "endNpcIds"));
		}
	}

	@Test
	void compiledAngelToTheWoundedPreservesTalkOrderAndQuestItem() {
		DataDrivenQuestData definition = definition(11010);
		assertEquals(List.of(798931), definition.getStartIds());
		assertEquals(List.of(799071), definition.getEndNpcIds());
		assertEquals(List.of(799071, 798906, 730323), definition.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182206713, definition.getSteps().get(2).getGiveItemId());
		assertEquals(1, definition.getSteps().get(2).getGiveItemCount());
	}

	@Test
	void compiledRemoteFoodPreservesSplitQuestItemDelivery() {
		DataDrivenQuestData definition = definition(2512);
		assertEquals(List.of(204703), definition.getStartIds());
		assertEquals(List.of(204753), definition.getEndNpcIds());
		assertEquals(182204411, definition.getStartGiveItemId());
		assertEquals(2, definition.getStartGiveItemCount());
		assertEquals(List.of(204801), definition.getSteps().get(0).getIds());
		assertEquals(182204411, definition.getSteps().get(0).getRemoveItemId());
		assertEquals(1, definition.getSteps().get(0).getRemoveItemCount());
	}

	@Test
	void compiledDemandNoteGivesDocumentAtIntermediateTalk() {
		DataDrivenQuestData definition = definition(1218);
		assertEquals(List.of(203121), definition.getStartIds());
		assertEquals(List.of(203172), definition.getEndNpcIds());
		assertEquals(List.of(798004), definition.getSteps().get(0).getIds());
		assertEquals(182200566, definition.getSteps().get(0).getGiveItemId());
		assertEquals(1, definition.getSteps().get(0).getGiveItemCount());
	}

	@Test
	void compiledSecretDeliverySwapsWorkItemsAtIntermediateTalk() {
		DataDrivenQuestData definition = definition(1220);
		assertEquals(List.of(203172), definition.getStartIds());
		assertEquals(List.of(205240), definition.getEndNpcIds());
		assertEquals(182200568, definition.getStartGiveItemId());
		assertEquals(1, definition.getStartGiveItemCount());
		assertEquals(List.of(798004), definition.getSteps().get(0).getIds());
		assertEquals(182200569, definition.getSteps().get(0).getGiveItemId());
		assertEquals(182200568, definition.getSteps().get(0).getRemoveItemId());
	}

	@Test
	void compiledHarumonerkRequestGivesBothWorkItemsInOrder() {
		DataDrivenQuestData definition = definition(1483);
		assertEquals(List.of(798126), definition.getStartIds());
		assertEquals(List.of(798127), definition.getEndNpcIds());
		assertEquals(List.of(203940, 203944), definition.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(List.of(182201401, 182201402), definition.getSteps().stream().map(DataDrivenQuestData.Step::getGiveItemId).toList());
	}

	@Test
	void compiledChiyorinrinerkRequestGivesThreeWorkItemsInOrder() {
		DataDrivenQuestData definition = definition(1484);
		assertEquals(List.of(798127), definition.getStartIds());
		assertEquals(List.of(798126), definition.getEndNpcIds());
		assertEquals(List.of(204045, 204048, 204011), definition.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(List.of(182201403, 182201404, 182201405), definition.getSteps().stream().map(DataDrivenQuestData.Step::getGiveItemId).toList());
	}

	@Test
	void compiledHeironDeliveriesPreserveItemDistribution() {
		DataDrivenQuestData mirror = definition(1553);
		assertEquals(List.of(203786), mirror.getStartIds());
		assertEquals(List.of(204584), mirror.getEndNpcIds());
		assertEquals(182201794, mirror.getStartGiveItemId());
		assertEquals(List.of(730051, 204500), mirror.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182201795, mirror.getSteps().get(0).getGiveItemId());
		assertEquals(182201794, mirror.getSteps().get(0).getRemoveItemId());

		DataDrivenQuestData village = definition(1574);
		assertEquals(List.of(730025), village.getStartIds());
		assertEquals(village.getStartIds(), village.getEndNpcIds());
		assertEquals(6, village.getStartGiveItemCount());
		assertEquals(List.of(204560, 204561, 204562), village.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(List.of(2, 2, 2), village.getSteps().stream().map(DataDrivenQuestData.Step::getRemoveItemCount).toList());
	}

	@Test
	void compiledAltgardLycanChainPreservesDocumentsAndNpcOrder() {
		DataDrivenQuestData interpreter = definition(2207);
		assertEquals(List.of(203590), interpreter.getStartIds());
		assertEquals(List.of(203591), interpreter.getEndNpcIds());
		assertEquals(182203257, interpreter.getStartGiveItemId());
		assertEquals(List.of(203591, 203557), interpreter.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182203257, interpreter.getSteps().get(1).getRemoveItemId());

		DataDrivenQuestData proposal = definition(2278);
		assertEquals(List.of(203590), proposal.getStartIds());
		assertEquals(List.of(203557), proposal.getEndNpcIds());
		assertEquals(List.of(203557, 204206, 204075), proposal.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182203254, proposal.getSteps().get(0).getGiveItemId());
		assertEquals(182203254, proposal.getSteps().get(2).getRemoveItemId());

		DataDrivenQuestData proof = definition(2279);
		assertEquals(List.of(203557), proof.getStartIds());
		assertEquals(proof.getStartIds(), proof.getEndNpcIds());
		assertEquals(List.of(203590, 203682), proof.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182203261, proof.getSteps().get(1).getGiveItemId());
	}

	@Test
	void compiledBeluslanDeliveriesPreserveItemDistribution() {
		DataDrivenQuestData errand = definition(2515);
		assertEquals(List.of(790015), errand.getStartIds());
		assertEquals(errand.getStartIds(), errand.getEndNpcIds());
		assertEquals(182204412, errand.getStartGiveItemId());
		assertEquals(List.of(204192, 204205, 798081), errand.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182204412, errand.getSteps().get(0).getRemoveItemId());
		assertEquals(List.of(182204414, 182204416), errand.getSteps().subList(1, 3).stream().map(DataDrivenQuestData.Step::getGiveItemId).toList());

		DataDrivenQuestData grass = definition(2523);
		assertEquals(List.of(204802), grass.getStartIds());
		assertEquals(List.of(204734), grass.getEndNpcIds());
		assertEquals(3, grass.getStartGiveItemCount());
		assertEquals(List.of(798117, 798118, 798119), grass.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(List.of(1, 1, 1), grass.getSteps().stream().map(DataDrivenQuestData.Step::getRemoveItemCount).toList());
	}

	@Test
	void compiledBeluslanObjectDeliveriesPreserveItemExchange() {
		DataDrivenQuestData musicBox = definition(2692);
		assertEquals(List.of(212164), musicBox.getStartIds());
		assertEquals(musicBox.getStartIds(), musicBox.getEndNpcIds());
		assertEquals(182204510, musicBox.getStartGiveItemId());
		assertEquals(List.of(204108, 279027, 279029), musicBox.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182204511, musicBox.getSteps().get(2).getGiveItemId());
		assertEquals(182204510, musicBox.getSteps().get(2).getRemoveItemId());

		DataDrivenQuestData order = definition(4501);
		assertEquals(List.of(204728), order.getStartIds());
		assertEquals(order.getStartIds(), order.getEndNpcIds());
		assertEquals(List.of(204340, 204348), order.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182204533, order.getSteps().get(1).getGiveItemId());
	}

	@Test
	void compiledCrossRegionDeliveriesPreserveObjectsAndItems() {
		DataDrivenQuestData stone = definition(3035);
		assertEquals(List.of(798155), stone.getStartIds());
		assertEquals(stone.getStartIds(), stone.getEndNpcIds());
		assertEquals(List.of(203830, 279029), stone.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182208024, stone.getStartGiveItemId());
		assertEquals(182208025, stone.getSteps().get(1).getGiveItemId());
		assertEquals(182208024, stone.getSteps().get(1).getRemoveItemId());

		DataDrivenQuestData messenger = definition(3973);
		assertEquals(List.of(203893), messenger.getStartIds());
		assertEquals(List.of(798949), messenger.getEndNpcIds());
		assertEquals(List.of(203792, 203793, 798391), messenger.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(List.of(182206116, 182206117, 182206118), messenger.getSteps().stream().map(DataDrivenQuestData.Step::getGiveItemId).toList());

		DataDrivenQuestData friend = definition(4052);
		assertEquals(List.of(730152), friend.getStartIds());
		assertEquals(friend.getStartIds(), friend.getEndNpcIds());
		assertEquals(List.of(205179, 205166, 205197), friend.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182209030, friend.getSteps().get(1).getGiveItemId());
		assertEquals(182209030, friend.getSteps().get(2).getRemoveItemId());

		RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(friend);
		Player player = playerWithState(4052, 0);
		state(player, 4052).setStatus(QuestStatus.NONE);
		assertTrue(quest.onDialogEvent(env(player, 4052, 730152, QuestDialog.USE_OBJECT, -1)));
		assertEquals(QuestStatus.NONE, state(player, 4052).getStatus());
	}

	@Test
	void compiledLostLoveAndWandererStaffGiveWorkItemAtIntermediateTalk() {
		for (var entry : Map.of(2914, List.of(204147, 204236, 182207014), 3037, List.of(798166, 798199, 182208027)).entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals(List.of(entry.getValue().get(0)), definition.getStartIds());
			assertEquals(definition.getStartIds(), definition.getEndNpcIds());
			assertEquals(List.of(entry.getValue().get(1)), definition.getSteps().get(0).getIds());
			assertEquals(entry.getValue().get(2), definition.getSteps().get(0).getGiveItemId());
			assertEquals(1, definition.getSteps().get(0).getGiveItemCount());
		}
	}

	@Test
	void compiledSpiritLetterStartsFromItemAndExchangesItAtGunter() {
		DataDrivenQuestData definition = definition(2321);
		assertEquals("ITEM_PLAY", definition.getStartType());
		assertEquals(182204242, definition.getStartItemId());
		assertEquals(List.of(790018), definition.getEndNpcIds());
		assertEquals(List.of(204225), definition.getSteps().get(0).getIds());
		assertEquals(182204119, definition.getSteps().get(0).getGiveItemId());
		assertEquals(182204242, definition.getSteps().get(0).getRemoveItemId());
	}

	@Test
	void compiledMorheimDeliveriesPreserveTalkOrderAndItemExchange() {
		DataDrivenQuestData book = definition(2428);
		assertEquals(List.of(204433), book.getStartIds());
		assertEquals(book.getStartIds(), book.getEndNpcIds());
		assertEquals(List.of(204102, 204211), book.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182204216, book.getSteps().get(1).getGiveItemId());

		DataDrivenQuestData roast = definition(2458);
		assertEquals(List.of(204379), roast.getStartIds());
		assertEquals(roast.getStartIds(), roast.getEndNpcIds());
		assertEquals(182204194, roast.getStartGiveItemId());
		assertEquals(List.of(204386), roast.getSteps().get(0).getIds());
		assertEquals(182204195, roast.getSteps().get(0).getGiveItemId());
		assertEquals(182204194, roast.getSteps().get(0).getRemoveItemId());
	}

	@Test
	void compiledMorheimAppearanceAndElimDeliveriesPreserveRetailActions() {
		DataDrivenQuestData appearance = definition(2421);
		assertEquals(132, appearance.getQuestMovie());
		assertEquals(182204208, appearance.getStartGiveItemId());
		assertEquals(182204209, appearance.getSteps().get(0).getGiveItemId());
		assertEquals(182204208, appearance.getSteps().get(0).getRemoveItemId());

		DataDrivenQuestData elim = definition(2480);
		assertEquals(List.of(730038), elim.getStartIds());
		assertEquals(elim.getStartIds(), elim.getEndNpcIds());
		assertEquals(List.of(730021, 730019), elim.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(List.of(182204201, 182204202), elim.getSteps().stream().map(DataDrivenQuestData.Step::getGiveItemId).toList());
	}

	@Test
	void compiledMovieTalksPreserveItemExchangeAndObjectTarget() {
		DataDrivenQuestData agrint = definition(3020);
		assertEquals(363, agrint.getQuestMovie());
		assertEquals(182208011, agrint.getStartGiveItemId());
		assertEquals(List.of(798149), agrint.getSteps().get(0).getIds());
		assertEquals(182208011, agrint.getSteps().get(0).getRemoveItemId());

		DataDrivenQuestData labourers = definition(4015);
		assertEquals(394, labourers.getQuestMovie());
		assertEquals(List.of(205130), labourers.getStartIds());
		assertEquals(labourers.getStartIds(), labourers.getEndNpcIds());
		assertEquals(List.of(730107), labourers.getSteps().get(0).getIds());
	}

	@Test
	void compiledAethericFieldDeliveryUsesAuthoritativeNpcOrder() {
		DataDrivenQuestData definition = definition(3076);
		assertEquals(List.of(798155), definition.getStartIds());
		assertEquals(definition.getStartIds(), definition.getEndNpcIds());
		assertEquals(List.of(278503, 278556), definition.getSteps().stream().map(step -> step.getIds().get(0)).toList());
		assertEquals(182208047, definition.getSteps().get(1).getGiveItemId());
	}

	@Test
	void compiledRetailBuyerQuestsUseItemCollectingWithoutSyntheticWorkItems() throws Exception {
		Map<Integer, Integer> expectedNpcIds = Map.of(
				19079, 805716, 19080, 805720, 19081, 805722,
				29079, 805717, 29080, 805721, 29081, 805723);
		for (var expected : expectedNpcIds.entrySet()) {
			XMLQuest definition = retailQuests.getQuest().stream()
					.filter(quest -> quest.getId() == expected.getKey())
					.findFirst()
					.orElseThrow();
			assertTrue(definition instanceof ItemCollectingData);
			assertEquals(List.of(expected.getValue()), readField(definition, "startNpcIds"));
			assertEquals(List.of(expected.getValue()), readField(definition, "endNpcIds"));
			assertEquals(0, readField(definition, "itemId"));
		}
	}

	@Test
	void compiledBridgeMoviesPlayAfterSimpleAccept() throws Exception {
		Map<Integer, List<Integer>> expected = Map.of(
				16979, List.of(802025, 801762, 886),
				26979, List.of(802026, 801764, 887));
		for (var entry : expected.entrySet()) {
			XMLQuest definition = retailQuests.getQuest().stream()
					.filter(quest -> quest.getId() == entry.getKey())
					.findFirst()
					.orElseThrow();
			assertTrue(definition instanceof ItemCollectingData);
			assertEquals(List.of(entry.getValue().get(0)), readField(definition, "startNpcIds"));
			assertEquals(List.of(entry.getValue().get(1)), readField(definition, "endNpcIds"));
			assertEquals(entry.getValue().get(2), definition.getQuestMovie());
			assertEquals(0, readField(definition, "itemId"));

			RecordingItemCollecting quest = new RecordingItemCollecting(entry.getKey(), entry.getValue().get(2));
			Player player = playerWithState(entry.getKey(), 0);
			state(player, entry.getKey()).setStatus(QuestStatus.NONE);
			assertTrue(quest.onDialogEvent(env(player, entry.getKey(), 1, QuestDialog.ACCEPT_QUEST_SIMPLE, 0)));
			assertEquals(entry.getValue().get(2), quest.playedMovie);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void compiledFireworkQuestsPreserveNpcOrderAndTimeout() throws Exception {
		Map<Integer, List<Integer>> expectedNpcIds = Map.of(
				80761, List.of(833648, 702947, 702948),
				80766, List.of(833650, 702950, 702951));
		for (var expected : expectedNpcIds.entrySet()) {
			XMLQuest definition = retailQuests.getQuest().stream()
					.filter(quest -> quest.getId() == expected.getKey())
					.findFirst()
					.orElseThrow();
			assertTrue(definition instanceof ReportToManyData);
			assertEquals(List.of(expected.getValue().get(0)), readField(definition, "startNpcIds"));
			assertEquals(List.of(expected.getValue().get(0)), readField(definition, "endNpcIds"));
			assertEquals(120, readField(definition, "timeoutSeconds"));
			assertEquals(1, readField(definition, "timeoutStartVar"));
			assertEquals(0, readField(definition, "timeoutResetVar"));
			List<NpcInfos> steps = (List<NpcInfos>) readField(definition, "npcInfos");
			assertEquals(expected.getValue().subList(1, 3), steps.stream().map(NpcInfos::getNpcId).toList());
			assertEquals(List.of(0, 1), steps.stream().map(NpcInfos::getVar).toList());
			assertEquals(List.of(1352, 1693), steps.stream().map(NpcInfos::getQuestDialog).toList());
		}
	}

	@Test
	void compiledFireworkTimeoutResetsFirstTalk() {
		RecordingReportToMany quest = new RecordingReportToMany();
		Player player = playerWithState(80761, 1);

		assertTrue(quest.onQuestTimerEndEvent(env(player, 80761, 0, QuestDialog.NULL, 0)));
		assertEquals(0, state(player, 80761).getQuestVarById(0));
	}

	@Test
	void compiledDebrisRescuesPreserveActionThenTalkState() {
		for (int questId : List.of(30503, 30553)) {
			DataDrivenQuestData definition = definition(questId);
			DataDrivenQuestData.Step step = definition.getSteps().get(0);
			assertEquals(List.of(205438), definition.getStartIds());
			assertEquals(List.of(205438), definition.getEndNpcIds());
			assertEquals("TALK", step.getType());
			assertEquals(List.of(799541), step.getIds());
			assertEquals(List.of(701097), step.getActionIds());
			assertTrue(step.isDeleteActionTarget());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(questId, 0);
			assertTrue(quest.onDialogEvent(env(player, questId, 701097, QuestDialog.NULL, -1)));
			assertEquals(0, state(player, questId).getQuestVarById(0));
			assertTrue(quest.onDialogEvent(env(player, questId, 799541, QuestDialog.NULL, 10000)));
			assertEquals(QuestStatus.REWARD, state(player, questId).getStatus());
		}
	}

	@Test
	void compiledWorldCollectsStayActiveOnlyInSteelRake() {
		for (int questId : List.of(3219, 3220, 4219, 4220)) {
			DataDrivenQuestData definition = definition(questId);
			assertEquals("WORLD_ACTIVE", definition.getStartType());
			assertEquals(300100000, definition.getWorldId());
			assertEquals(List.of(), definition.getEndNpcIds());
			assertEquals(List.of(), definition.getSteps());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(questId, 0);
			state(player, questId).setStatus(QuestStatus.NONE);
			player.setPosition(new WorldPosition(300100000));
			assertTrue(quest.onEnterWorldEvent(env(player, questId, 0, QuestDialog.NULL, 0)));
			assertEquals(QuestStatus.START, state(player, questId).getStatus());
			state(player, questId).setQuestVarById(0, 7);
			assertFalse(quest.onEnterWorldEvent(env(player, questId, 0, QuestDialog.NULL, 0)));
			assertEquals(QuestStatus.START, state(player, questId).getStatus());
			assertEquals(7, state(player, questId).getQuestVarById(0));

			player.setPosition(new WorldPosition(210010000));
			assertTrue(quest.onEnterWorldEvent(env(player, questId, 0, QuestDialog.NULL, 0)));
			assertEquals(QuestStatus.NONE, state(player, questId).getStatus());
			assertEquals(0, state(player, questId).getQuestVarById(0));
		}
	}

	@Test
	void compiledGrowthQuestsAdvanceWhenEvolutionMaterialIsObtained() {
		Map<Integer, List<Integer>> expected = Map.of(
				19678, List.of(806698, 166020000), 19679, List.of(806698, 166030005),
				29678, List.of(806700, 166020000), 29679, List.of(806700, 166030005));
		for (var entry : expected.entrySet()) {
			int questId = entry.getKey();
			DataDrivenQuestData definition = definition(questId);
			DataDrivenQuestData.Step step = definition.getSteps().get(0);
			assertEquals(List.of(entry.getValue().get(0)), definition.getStartIds());
			assertEquals(List.of(entry.getValue().get(0)), definition.getEndNpcIds());
			assertEquals("GET_ITEM", step.getType());
			assertEquals(entry.getValue().get(1), step.getItemId());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(questId, 0);
			assertTrue(quest.onGetItemEvent(env(player, questId, 0, QuestDialog.NULL, 0)));
			assertEquals(QuestStatus.REWARD, state(player, questId).getStatus());
		}
	}

	@Test
	void compiledSensoryEpiloguesFinishWhenTheirAreaNpcIsSensed() {
		Map<Integer, Integer> expected = Map.of(3959, 206101, 4963, 206102);
		for (var entry : expected.entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals("SENSORY_COMPLETE", definition.getStartType());
			assertEquals(List.of(entry.getValue()), definition.getStartIds());
			assertEquals(List.of(), definition.getEndNpcIds());
			assertEquals(List.of(), definition.getSteps());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(entry.getKey(), 0);
			assertTrue(quest.onAddAggroListEvent(env(player, entry.getKey(), entry.getValue(), QuestDialog.NULL, 0)));
			assertTrue(quest.sensoryCompleted);
		}
	}

	@Test
	void compiledCoalescenceQuestsCompleteWhenAccepted() {
		Map<Integer, Integer> starts = Map.of(15542, 806074, 25542, 806078);
		for (var entry : starts.entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals("TALK", definition.getStartType());
			assertEquals(List.of(entry.getValue()), definition.getStartIds());
			assertEquals(List.of(entry.getValue()), definition.getEndNpcIds());
			assertTrue(definition.isCompleteOnStart());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			quest.startDialogStartsQuest = true;
			Player player = playerWithState(entry.getKey(), 0);
			state(player, entry.getKey()).setStatus(QuestStatus.NONE);
			assertTrue(quest.onDialogEvent(env(player, entry.getKey(), entry.getValue(), QuestDialog.ACCEPT_QUEST_SIMPLE, 0)));
			assertEquals(QuestStatus.REWARD, state(player, entry.getKey()).getStatus());
		}
	}

	@Test
	void compiledBastionMovieQuestsPlayMovieBeforeRewarding() {
		Map<Integer, List<Integer>> expected = Map.of(
				18036, List.of(801281, 802008), 28036, List.of(801280, 802015));
		for (var entry : expected.entrySet()) {
			int questId = entry.getKey();
			DataDrivenQuestData definition = definition(questId);
			assertEquals(List.of(entry.getValue().get(0)), definition.getStartIds());
			assertEquals(List.of(entry.getValue().get(0)), definition.getEndNpcIds());
			assertEquals(28, definition.getQuestMovie());
			assertEquals(List.of(entry.getValue().get(1)), definition.getSteps().get(0).getIds());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(questId, 0);
			assertTrue(quest.onDialogEvent(env(player, questId, entry.getValue().get(1), QuestDialog.NULL, 10000)));
			assertEquals(28, quest.playedMovie);
			assertEquals(QuestStatus.REWARD, state(player, questId).getStatus());
		}
	}

	@Test
	void retailSimpleTalkCutsceneUsesStepDialogAndMovie() {
		DataDrivenQuestData definition = definition(11074);
		DataDrivenQuestData.Step step = definition.getSteps().get(0);
		assertEquals(List.of(799025), step.getIds());
		assertEquals(2375, step.getDialogId());
		assertEquals(1009, step.getAdvanceDialogId());
		assertEquals(512, step.getMovie());

		RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
		Player player = playerWithState(11074, 0);
		assertTrue(quest.onDialogEvent(env(player, 11074, 799025, QuestDialog.SELECT_REWARD, 1009)));
		assertEquals(512, quest.playedMovie);
		assertTrue(quest.questEndDialogSent);
		assertEquals(QuestStatus.REWARD, state(player, 11074).getStatus());
	}

	@Test
	void typedStepActionsRunForNonTalkProgress() throws Exception {
		Unmarshaller unmarshaller = JAXBContext.newInstance(StaticData.class).createUnmarshaller();
		XMLQuests quests = (XMLQuests) unmarshaller.unmarshal(new StringReader("""
				<quest_scripts><data_driven_quest id="900001" retail="true" start_type="TALK" start_ids="1" end_npc_ids="2">
				  <step type="ENTER_AREA" ids="3" movie="502" teleport_world_id="210050000"
				      teleport_x="1440" teleport_y="407" teleport_z="553" teleport_heading="90">
				    <spawn npc_id="4" count="3" lifetime_seconds="30" x="10" y="20" z="30" heading="40"/>
				  </step>
				</data_driven_quest></quest_scripts>
				"""));
		DataDrivenQuestData definition = definition(quests, 900001);
		DataDrivenQuestData.Step step = definition.getSteps().get(0);
		assertEquals(210050000, step.getTeleportWorldId());
		assertEquals(90, step.getTeleportHeading());
		assertEquals(4, step.getSpawns().get(0).getNpcId());

		RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
		Player player = playerWithState(900001, 0);
		assertTrue(quest.onAddAggroListEvent(env(player, 900001, 3, QuestDialog.NULL, 0)));
		assertEquals(List.of(210050000, 1440, 407, 553, 90), quest.teleport);
		assertEquals(502, quest.playedMovie);
		assertEquals(3, quest.spawnCount);
		assertEquals(List.of(4, 30, 10.0f, 20.0f, 30.0f, 40), quest.spawn);
		assertEquals(QuestStatus.REWARD, state(player, 900001).getStatus());
	}

	@Test
	void archivesArrivalCutscenesUseRetailData() {
		for (var entry : Map.of(16800, List.of(806075, 806148, 206536, 806232, 931),
				26800, List.of(806079, 806149, 206537, 806233, 932)).entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals("TALK", definition.getStartType());
			assertEquals(List.of(entry.getValue().get(0)), definition.getStartIds());
			assertEquals(List.of(entry.getValue().get(1)), definition.getEndNpcIds());
			assertEquals(List.of(entry.getValue().get(2), entry.getValue().get(3), 206535),
					definition.getSteps().stream().map(step -> step.getIds().get(0)).toList());
			assertEquals(entry.getValue().get(4), definition.getSteps().get(2).getMovie());
		}
	}

	@Test
	void bastionSoulSpawnsUseRetailData() {
		for (var entry : Map.of(13954, List.of(806590, 806582, 203840, 182216179, 1939, 1768, 576, 55),
				23954, List.of(806599, 806591, 204153, 182216188, 988, 1161, 200, 29)).entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals("TALK", definition.getStartType());
			assertEquals(List.of(entry.getValue().get(0)), definition.getStartIds());
			assertEquals(List.of(entry.getValue().get(1)), definition.getEndNpcIds());
			assertEquals(List.of(entry.getValue().get(0), entry.getValue().get(2), 247093, entry.getValue().get(2)),
					definition.getSteps().stream().map(step -> step.getIds().get(0)).toList());
			assertEquals(entry.getValue().get(3), definition.getSteps().get(0).getGiveItemId());
			assertEquals(entry.getValue().get(3), definition.getSteps().get(1).getRemoveItemId());
			DataDrivenQuestData.Spawn spawn = definition.getSteps().get(1).getSpawns().get(0);
			assertEquals(247093, spawn.getNpcId());
			assertEquals(3, spawn.getCount());
			assertEquals(100, spawn.getLifetimeSeconds());
			assertEquals(entry.getValue().subList(4, 7), List.of((int) spawn.getX(), (int) spawn.getY(), (int) spawn.getZ()));
			assertEquals(entry.getValue().get(7), spawn.getHeading());
		}
	}

	@Test
	void compiledPaiosRescuesAdvanceAtColumnAndResetOnRentusReentry() {
		for (int questId : List.of(30504, 30554)) {
			DataDrivenQuestData definition = definition(questId);
			assertEquals("TALK", definition.getStartType());
			assertEquals(List.of(205438), definition.getStartIds());
			assertEquals(List.of(799536), definition.getEndNpcIds());
			assertEquals(300280000, definition.getResetWorldId());
			assertEquals("ACTION", definition.getSteps().get(0).getType());
			assertEquals(List.of(701098), definition.getSteps().get(0).getActionIds());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(questId, 0);
			player.setPosition(new WorldPosition(300620000));
			assertTrue(quest.onDialogEvent(env(player, questId, 701098, QuestDialog.NULL, -1)));
			assertEquals(QuestStatus.REWARD, state(player, questId).getStatus());
			assertEquals(1, state(player, questId).getQuestVarById(0));

			player.setPosition(new WorldPosition(300280000));
			assertTrue(quest.onEnterWorldEvent(env(player, questId, 0, QuestDialog.NULL, 0)));
			assertEquals(QuestStatus.START, state(player, questId).getStatus());
			assertEquals(0, state(player, questId).getQuestVarById(0));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void compiledSuramaHuntsUseThreeRetailTargetsAndFiveKills() throws Exception {
		for (int questId : List.of(30708, 30758)) {
			XMLQuest definition = retailQuests.getQuest().stream().filter(quest -> quest.getId() == questId).findFirst().orElseThrow();
			assertTrue(definition instanceof MonsterHuntData);
			assertEquals(List.of(800369), readField(definition, "startNpcIds"));
			assertEquals(List.of(800438), readField(definition, "endNpcIds"));
			List<Monster> monsters = (List<Monster>) readField(definition, "monster");
			assertEquals(1, monsters.size());
			assertEquals(List.of(800425, 800426, 800427), monsters.get(0).getNpcIds());
			assertEquals(5, monsters.get(0).getEndVar());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void compiledChristmasCourierHuntsUseRetailSantaAndTwoKills() throws Exception {
		for (var entry : Map.of(50008, List.of(831032, 219290), 51008, List.of(831033, 219291)).entrySet()) {
			XMLQuest definition = retailQuests.getQuest().stream().filter(quest -> quest.getId() == entry.getKey()).findFirst().orElseThrow();
			assertTrue(definition instanceof MonsterHuntData);
			assertEquals(List.of(entry.getValue().get(0)), readField(definition, "startNpcIds"));
			assertEquals(null, readField(definition, "endNpcIds"));
			List<Monster> monsters = (List<Monster>) readField(definition, "monster");
			assertEquals(1, monsters.size());
			assertEquals(List.of(entry.getValue().get(1)), monsters.get(0).getNpcIds());
			assertEquals(2, monsters.get(0).getEndVar());
		}
	}

	@Test
	void compiledArenaItemPlaysPreserveTalkSwapUseAndRewardOrder() throws Exception {
		Map<Integer, List<Integer>> expected = Map.of(
				18213, List.of(205985, 205316, 798604, 182212219, 182212220),
				28213, List.of(205986, 205320, 798804, 182212222, 182212223));
		for (var entry : expected.entrySet()) {
			int questId = entry.getKey();
			List<Integer> values = entry.getValue();
			DataDrivenQuestData definition = definition(questId);
			assertEquals(List.of(values.get(0)), definition.getStartIds());
			assertEquals(List.of(values.get(2)), definition.getEndNpcIds());
			assertEquals(values.get(3), definition.getStartGiveItemId());
			assertEquals(List.of("TALK", "TALK", "ITEM_PLAY"), definition.getSteps().stream().map(DataDrivenQuestData.Step::getType).toList());
			assertEquals(List.of(values.get(1)), definition.getSteps().get(0).getIds());
			assertEquals(List.of(values.get(2)), definition.getSteps().get(1).getIds());
			assertEquals(values.get(4), definition.getSteps().get(1).getGiveItemId());
			assertEquals(values.get(3), definition.getSteps().get(1).getRemoveItemId());
			assertEquals(values.get(4), definition.getSteps().get(2).getItemId());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(questId, 0);
			assertTrue(quest.onDialogEvent(env(player, questId, values.get(1), QuestDialog.NULL, 10000)));
			assertEquals(1, state(player, questId).getQuestVarById(0));
			state(player, questId).setQuestVarById(0, 2);
			assertSame(HandlerResult.SUCCESS, quest.onItemUseEvent(env(player, questId, 0, QuestDialog.NULL, 0), item(values.get(4))));
			assertEquals(QuestStatus.REWARD, state(player, questId).getStatus());
		}
	}

	@Test
	void compiledDredgionControlHuntsAcceptObjectUseBeforeKill() {
		for (int questId : List.of(30702, 30752)) {
			DataDrivenQuestData definition = definition(questId);
			assertEquals(List.of(800424), definition.getStartIds());
			assertEquals(List.of(800461), definition.getEndNpcIds());
			assertEquals(List.of("TALK", "HUNT"), definition.getSteps().stream().map(DataDrivenQuestData.Step::getType).toList());
			assertEquals(List.of(730702), definition.getSteps().get(0).getIds());
			assertEquals(List.of(219354), definition.getSteps().get(1).getIds());
			assertEquals(1, definition.getSteps().get(1).getAmount());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(questId, 0);
			assertTrue(quest.onDialogEvent(env(player, questId, 730702, QuestDialog.USE_OBJECT, 0)));
			assertTrue(quest.onDialogEvent(env(player, questId, 730702, QuestDialog.STEP_TO_1, 10000)));
			assertEquals(1, state(player, questId).getQuestVarById(0));
			assertTrue(quest.onKillEvent(env(player, questId, 219354, QuestDialog.NULL, 0)));
			assertEquals(QuestStatus.REWARD, state(player, questId).getStatus());
		}
	}

	@Test
	void compiledDredgionNavigationHuntsPreserveSerialTargets() {
		for (var entry : Map.of(30600, List.of(800325, 800324), 30610, List.of(800327, 800326)).entrySet()) {
			int questId = entry.getKey();
			DataDrivenQuestData definition = definition(questId);
			assertEquals(List.of(entry.getValue().get(0)), definition.getStartIds());
			assertEquals(definition.getStartIds(), definition.getEndNpcIds());
			assertEquals(List.of("TALK", "HUNT", "HUNT"), definition.getSteps().stream().map(DataDrivenQuestData.Step::getType).toList());
			assertEquals(List.of(entry.getValue().get(1)), definition.getSteps().get(0).getIds());
			assertEquals(List.of(219256, 219257), definition.getSteps().get(1).getIds());
			assertEquals(List.of(219264), definition.getSteps().get(2).getIds());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(questId, 0);
			assertTrue(quest.onDialogEvent(env(player, questId, entry.getValue().get(1), QuestDialog.STEP_TO_1, 10000)));
			assertFalse(quest.onKillEvent(env(player, questId, 219264, QuestDialog.NULL, 0)));
			assertTrue(quest.onKillEvent(env(player, questId, 219257, QuestDialog.NULL, 0)));
			assertEquals(2, state(player, questId).getQuestVarById(0));
			assertTrue(quest.onKillEvent(env(player, questId, 219264, QuestDialog.NULL, 0)));
			assertEquals(QuestStatus.REWARD, state(player, questId).getStatus());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void repeatedTalkAndChallengeTasksPreserveRetailSemantics() throws Exception {
		Map<Integer, List<Integer>> talkSteps = Map.of(
				1888, List.of(278591, 278651, 278578, 278651, 278592),
				2888, List.of(278086, 278151, 278085, 278151, 278087));
		for (var entry : talkSteps.entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals(entry.getValue(), definition.getSteps().stream().map(step -> step.getIds().get(0)).toList());
			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(entry.getKey(), 0);
			for (int index = 0; index < entry.getValue().size(); index++) {
				assertTrue(quest.onDialogEvent(env(player, entry.getKey(), entry.getValue().get(index), QuestDialog.NULL, 10000 + index)));
			}
			assertEquals(QuestStatus.REWARD, state(player, entry.getKey()).getStatus());
		}

		Map<Integer, List<Integer>> huntTargets = Map.of(
				17160, List.of(235830, 235912, 235916), 17161, List.of(219699, 219776, 219787),
				27160, List.of(219700, 219777, 219788), 27161, List.of(235832, 235914, 235918));
		for (var entry : huntTargets.entrySet()) {
			XMLQuest definition = retailQuests.getQuest().stream().filter(quest -> quest.getId() == entry.getKey()).findFirst().orElseThrow();
			assertTrue(definition instanceof MonsterHuntData);
			assertEquals(List.of(0), readField(definition, "startNpcIds"));
			List<Monster> monsters = (List<Monster>) readField(definition, "monster");
			assertEquals(1, monsters.size());
			assertEquals(10, monsters.get(0).getEndVar());
			assertEquals(entry.getValue(), monsters.get(0).getNpcIds());
		}
	}

	@Test
	void compiledHousingFlowerVisitsUseAllButlersAndRetailTalkOrder() {
		Map<Integer, List<List<Integer>>> expected = Map.of(
				18806, List.of(List.of(810017, 810018, 810019, 810020, 810021), List.of(830528), List.of(830194)),
				28806, List.of(List.of(810022, 810023, 810024, 810025, 810026), List.of(830530), List.of(830211)));
		for (var entry : expected.entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals("TALK", definition.getStartType());
			assertEquals(entry.getValue().get(0), definition.getStartIds());
			assertEquals(entry.getValue().get(1), definition.getSteps().get(0).getIds());
			assertEquals(entry.getValue().get(2), definition.getEndNpcIds());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(entry.getKey(), 0);
			assertTrue(quest.onDialogEvent(env(player, entry.getKey(), entry.getValue().get(1).get(0), QuestDialog.NULL, 10000)));
			assertEquals(QuestStatus.REWARD, state(player, entry.getKey()).getStatus());
		}
	}

	@Test
	void compiledScorchedTreesPreserveRetailActionOrder() {
		Map<Integer, Integer> starts = Map.of(13809, 802427, 23809, 802429);
		List<Integer> actions = List.of(730969, 730970, 730971);
		for (var entry : starts.entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals(List.of(entry.getValue()), definition.getStartIds());
			assertEquals(List.of(entry.getValue()), definition.getEndNpcIds());
			assertEquals(actions, definition.getSteps().stream().map(step -> step.getActionIds().get(0)).toList());
			assertTrue(definition.getSteps().stream().allMatch(step -> "ACTION".equals(step.getType())));

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(entry.getKey(), 0);
			for (int action : actions) {
				assertTrue(quest.onDialogEvent(env(player, entry.getKey(), action, QuestDialog.NULL, -1)));
			}
			assertEquals(QuestStatus.REWARD, state(player, entry.getKey()).getStatus());
			assertEquals(3, state(player, entry.getKey()).getQuestVarById(0));
		}
	}

	@Test
	void compiledKaldorArrivalsUseRetailTalkChain() {
		Map<Integer, List<Integer>> expected = Map.of(13800, List.of(804699, 804782, 802431), 23800, List.of(804719, 804753, 802433));
		for (var entry : expected.entrySet()) {
			DataDrivenQuestData definition = definition(entry.getKey());
			assertEquals(List.of(entry.getValue().get(0)), definition.getStartIds());
			assertEquals(List.of(entry.getValue().get(2)), definition.getEndNpcIds());
			assertEquals(List.of(entry.getValue().get(1)), definition.getSteps().get(0).getIds());

			RecordingDataDrivenQuest quest = new RecordingDataDrivenQuest(definition);
			Player player = playerWithState(entry.getKey(), 0);
			assertTrue(quest.onDialogEvent(env(player, entry.getKey(), entry.getValue().get(1), QuestDialog.NULL, 10000)));
			assertEquals(QuestStatus.REWARD, state(player, entry.getKey()).getStatus());
		}
	}

	@Test
	void talkStepAdvancesRetailQuest1118() {
		RecordingDataDrivenQuest quest = quest(1118);
		Player player = playerWithState(1118, 0);

		assertTrue(quest.onDialogEvent(env(player, 1118, 203070, QuestDialog.NULL, 10000)));
		assertEquals(QuestStatus.REWARD, state(player, 1118).getStatus());
	}

	@Test
	void collectItemStepDispatchesRetailQuest1152() {
		RecordingDataDrivenQuest quest = quest(1152);
		Player player = playerWithState(1152, 1);

		assertTrue(quest.onDialogEvent(env(player, 1152, 203130, QuestDialog.CHECK_COLLECTED_ITEMS, 0)));
		assertTrue(quest.collectItemsChecked);
		assertEquals(QuestStatus.REWARD, state(player, 1152).getStatus());
	}

	@Test
	void huntStepsAdvanceRetailQuest14252() {
		RecordingDataDrivenQuest quest = quest(14252);
		Player player = playerWithState(14252, 0);

		for (int npcId : List.of(236924, 237263, 237275)) {
			assertTrue(quest.onKillEvent(env(player, 14252, npcId, QuestDialog.NULL, 0)));
		}
		assertEquals(QuestStatus.REWARD, state(player, 14252).getStatus());
	}

	@Test
	void pvpKillsAdvanceRetailQuest11324() {
		RecordingKillInWorld quest = new RecordingKillInWorld(11324, 2);
		Player player = playerWithState(11324, 0);
		QuestEnv env = env(player, 11324, 0, QuestDialog.NULL, 0);

		assertTrue(quest.onKillInWorldEvent(env));
		assertEquals(QuestStatus.START, state(player, 11324).getStatus());
		assertTrue(quest.onKillInWorldEvent(env));
		assertEquals(QuestStatus.REWARD, state(player, 11324).getStatus());
	}

	@Test
	void itemPlayAdvancesRetailQuest13951() throws Exception {
		RecordingDataDrivenQuest quest = quest(13951);
		Player player = playerWithState(13951, 0);

		assertSame(HandlerResult.SUCCESS, quest.onItemUseEvent(
				env(player, 13951, 0, QuestDialog.NULL, 0), item(182216201)));
		assertEquals(QuestStatus.REWARD, state(player, 13951).getStatus());
	}

	@Test
	void enterAreaAdvancesRetailQuest15322() {
		RecordingDataDrivenQuest quest = quest(15322);
		Player player = playerWithState(15322, 0);

		assertTrue(quest.onAddAggroListEvent(env(player, 15322, 206465, QuestDialog.NULL, 0)));
		assertEquals(1, state(player, 15322).getQuestVarById(0));
	}

	@Test
	void enterWorldAdvancesRetailQuest13950() {
		RecordingDataDrivenQuest quest = quest(13950);
		Player player = playerWithState(13950, 2);
		player.setPosition(new WorldPosition(302340000));

		assertTrue(quest.onEnterWorldEvent(env(player, 13950, 0, QuestDialog.NULL, 0)));
		assertEquals(QuestStatus.REWARD, state(player, 13950).getStatus());
	}

	@Test
	void mixedRetailQuest17540PreservesStepOrder() {
		RecordingDataDrivenQuest quest = quest(17540);
		Player player = playerWithState(17540, 0);
		player.setPosition(new WorldPosition(300250000));

		assertTrue(quest.onEnterWorldEvent(env(player, 17540, 0, QuestDialog.NULL, 0)));
		assertTrue(quest.onDialogEvent(env(player, 17540, 799563, QuestDialog.NULL, 10001)));
		assertTrue(quest.onKillEvent(env(player, 17540, 217185, QuestDialog.NULL, 0)));
		assertTrue(quest.onKillEvent(env(player, 17540, 217195, QuestDialog.NULL, 0)));
		assertTrue(quest.onDialogEvent(env(player, 17540, 799553, QuestDialog.CHECK_COLLECTED_ITEMS, 0)));
		assertEquals(QuestStatus.REWARD, state(player, 17540).getStatus());
	}

	private static RecordingDataDrivenQuest quest(int questId) {
		return new RecordingDataDrivenQuest(definition(questId));
	}

	private static DataDrivenQuestData definition(int questId) {
		return definition(retailQuests, questId);
	}

	private static DataDrivenQuestData definition(XMLQuests quests, int questId) {
		XMLQuest definition = quests.getQuest().stream()
				.filter(quest -> quest.getId() == questId)
				.findFirst()
				.orElseThrow();
		assertTrue(definition instanceof DataDrivenQuestData);
		return (DataDrivenQuestData) definition;
	}

	private static Player playerWithState(int questId, int step) {
		Player player = new ObjenesisStd().newInstance(Player.class);
		QuestStateList states = new QuestStateList();
		QuestState state = new NonRepeatableQuestState(questId);
		state.setQuestVarById(0, step);
		states.addQuest(questId, state);
		player.setQuestStateList(states);
		return player;
	}

	private static QuestState state(Player player, int questId) {
		return player.getQuestStateList().getQuestState(questId);
	}

	private static QuestEnv env(Player player, int questId, int targetId, QuestDialog dialog, int dialogId) {
		return new FixedQuestEnv(player, questId, targetId, dialog, dialogId);
	}

	private static Item item(int itemId) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setField(template, "itemId", itemId);
		Item item = new ObjenesisStd().newInstance(Item.class);
		setField(item, "itemTemplate", template);
		return item;
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Object readField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static final class NonRepeatableQuestState extends QuestState {
		private NonRepeatableQuestState(int questId) {
			super(questId, QuestStatus.START, 0, 0, null, 0, null);
		}

		@Override
		public boolean canRepeat() {
			return false;
		}
	}

	private static final class RecordingItemCollecting extends ItemCollecting {

		private int playedMovie;

		private RecordingItemCollecting(int questId, int movie) {
			super(questId, List.of(1), 0, null, List.of(2), movie, 0, 0, 0, 5, 2716, 0);
		}

		@Override
		public boolean sendQuestStartDialog(QuestEnv env) {
			state(env.getPlayer(), getQuestId()).setStatus(QuestStatus.START);
			return true;
		}

		@Override
		public boolean playQuestMovie(QuestEnv env, int movieId) {
			playedMovie = movieId;
			return true;
		}
	}

	private static final class RecordingDataDrivenQuest extends DataDrivenQuest {

		private boolean collectItemsChecked;
		private boolean questEndDialogSent;
		private boolean sensoryCompleted;
		private boolean startDialogStartsQuest;
		private int playedMovie;
		private int shownDialogId;
		private int givenItemId;
		private int givenItemCount;
		private int spawnCount;
		private List<Number> spawn;
		private List<Integer> teleport;

		private RecordingDataDrivenQuest(DataDrivenQuestData data) {
			super(data);
		}

		@Override
		public synchronized void updateQuestStatus(QuestEnv env) {
		}

		@Override
		public boolean closeDialogWindow(QuestEnv env) {
			return true;
		}

		@Override
		public boolean sendQuestStartDialog(QuestEnv env) {
			if (startDialogStartsQuest) {
				state(env.getPlayer(), getQuestId()).setStatus(QuestStatus.START);
				return true;
			}
			return super.sendQuestStartDialog(env);
		}

		@Override
		public boolean sendQuestDialog(QuestEnv env, int dialogId) {
			shownDialogId = dialogId;
			return true;
		}

		@Override
		public boolean sendQuestEndDialog(QuestEnv env) {
			questEndDialogSent = true;
			return true;
		}

		@Override
		public boolean giveQuestItem(QuestEnv env, int itemId, int itemCount) {
			givenItemId = itemId;
			givenItemCount = itemCount;
			return true;
		}

		@Override
		public boolean playQuestMovie(QuestEnv env, int movieId) {
			playedMovie = movieId;
			return true;
		}

		@Override
		protected boolean teleport(QuestEnv env, DataDrivenQuestData.Step step) {
			teleport = List.of(step.getTeleportWorldId(), step.getTeleportX(), step.getTeleportY(), step.getTeleportZ(), step.getTeleportHeading());
			return true;
		}

		@Override
		protected void spawn(QuestEnv env, DataDrivenQuestData.Spawn spawn) {
			spawnCount++;
			this.spawn = List.of(spawn.getNpcId(), spawn.getLifetimeSeconds(), spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getHeading());
		}

		@Override
		protected boolean startWorldActive(QuestEnv env) {
			state(env.getPlayer(), getQuestId()).setStatus(QuestStatus.START);
			return true;
		}

		@Override
		protected boolean stopWorldActive(QuestEnv env) {
			QuestState state = state(env.getPlayer(), getQuestId());
			state.setStatus(QuestStatus.NONE);
			state.setQuestVar(0);
			return true;
		}

		@Override
		protected boolean completeSensory(QuestEnv env) {
			sensoryCompleted = true;
			return true;
		}

		@Override
		public boolean checkQuestItems(QuestEnv env, int step, int nextStep, boolean reward, int checkOkId, int checkFailId) {
			collectItemsChecked = true;
			QuestState state = state(env.getPlayer(), getQuestId());
			state.setQuestVarById(0, nextStep);
			if (reward) {
				state.setStatus(QuestStatus.REWARD);
			}
			return true;
		}
	}

	private static final class RecordingKillInWorld extends KillInWorld {

		private RecordingKillInWorld(int questId, int amount) {
			super(questId, List.of(1), List.of(1), List.of(1), amount, 0, 10002);
		}

		@Override
		public synchronized void updateQuestStatus(QuestEnv env) {
		}
	}

	private static final class RecordingReportToMany extends ReportToMany {

		private RecordingReportToMany() {
			super(80761, 0, List.of(833648), List.of(833648), Map.of(), 1011, 2375, 1, 120, 1, 0);
		}

		@Override
		public synchronized void updateQuestStatus(QuestEnv env) {
		}
	}

	private static final class FixedQuestEnv extends QuestEnv {

		private final int targetId;
		private final QuestDialog dialog;

		private FixedQuestEnv(Player player, int questId, int targetId, QuestDialog dialog, int dialogId) {
			super(null, player, questId, dialogId);
			this.targetId = targetId;
			this.dialog = dialog;
		}

		@Override
		public int getTargetId() {
			return targetId;
		}

		@Override
		public QuestDialog getDialog() {
			return dialog;
		}
	}
}
