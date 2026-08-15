package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.objenesis.ObjenesisStd;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestStartEligibilityPortTest {
	private static final int PLAYER_ID = 7;
	private int originalQuestLimit;

	@BeforeEach
	void setUp() {
		originalQuestLimit = CustomConfig.BASIC_QUEST_SIZE_LIMIT;
		CustomConfig.BASIC_QUEST_SIZE_LIMIT = 40;
	}

	@AfterEach
	void tearDown() {
		CustomConfig.BASIC_QUEST_SIZE_LIMIT = originalQuestLimit;
	}

	@Test
	void actualAutomaticMissionLevelsAndPrerequisitesAreStrictOnBothEntrypoints() throws Exception {
		QuestMetadata quest1001 = metadata(1001);
		QuestMetadata quest10501 = metadata(10501);
		Map<Integer, QuestMetadata> metadata = Map.of(1001, quest1001, 10501, quest10501);

		Player early1001 = player(1);
		PlayerQuestStartEligibilityPort earlyPort = port(early1001, metadata);
		assertRejected(earlyPort, 1001, new QuestEvent.LevelUp(), "MIN_LEVEL_NOT_MET");
		assertRejected(earlyPort, 1001, new QuestEvent.ZoneMissionEnd(), "MIN_LEVEL_NOT_MET");

		Player exact1001 = player(2);
		PlayerQuestStartEligibilityPort exactPort = port(exact1001, metadata);
		assertTrue(exactPort.snapshot(PLAYER_ID, 1001, new QuestEvent.LevelUp()).eligible());
		assertTrue(exactPort.snapshot(PLAYER_ID, 1001, new QuestEvent.ZoneMissionEnd()).eligible());

		Player early10501 = player(55);
		assertRejected(port(early10501, metadata), 10501, new QuestEvent.LevelUp(), "MIN_LEVEL_NOT_MET");
		Player exact10501 = player(56);
		PlayerQuestStartEligibilityPort prerequisitePort = port(exact10501, metadata);
		assertRejected(prerequisitePort, 10501, new QuestEvent.LevelUp(), "START_CONDITION_REJECTED");
		exact10501.getQuestStateList().addQuest(10500,
			new QuestState(10500, QuestStatus.COMPLETE, 0, 1, null, 0, null));
		assertTrue(prerequisitePort.snapshot(PLAYER_ID, 10501, new QuestEvent.LevelUp()).eligible());
		assertTrue(prerequisitePort.snapshot(PLAYER_ID, 10501, new QuestEvent.ZoneMissionEnd()).eligible());
	}

	@Test
	void quest10521RejectsLevelTenAndRequiresQuest10520AtLevelSixtyFive() throws Exception {
		QuestMetadata quest10521 = metadata(10521);
		Map<Integer, QuestMetadata> metadata = Map.of(10521, quest10521);

		Player levelTen = player(10);
		PlayerQuestStartEligibilityPort earlyPort = port(levelTen, metadata);
		assertRejected(earlyPort, 10521, new QuestEvent.LevelUp(), "MIN_LEVEL_NOT_MET");
		assertRejected(earlyPort, 10521, new QuestEvent.ZoneMissionEnd(), "MIN_LEVEL_NOT_MET");

		Player levelSixtyFive = player(65);
		PlayerQuestStartEligibilityPort prerequisitePort = port(levelSixtyFive, metadata);
		assertRejected(prerequisitePort, 10521, new QuestEvent.LevelUp(), "TITLE_MISSING");
		setField(Player.class, levelSixtyFive, "titleList", titleListWith(306));
		assertRejected(prerequisitePort, 10521, new QuestEvent.LevelUp(), "START_CONDITION_REJECTED");
		assertRejected(prerequisitePort, 10521, new QuestEvent.ZoneMissionEnd(), "START_CONDITION_REJECTED");
		levelSixtyFive.getQuestStateList().addQuest(10520,
			new QuestState(10520, QuestStatus.COMPLETE, 0, 1, null, 0, null));
		assertTrue(prerequisitePort.snapshot(PLAYER_ID, 10521, new QuestEvent.LevelUp()).eligible());
		assertTrue(prerequisitePort.snapshot(PLAYER_ID, 10521, new QuestEvent.ZoneMissionEnd()).eligible());
	}

	@Test
	void dispatchToVerteronRequiresTheMatchingClassAndQuest1007RewardBranch() throws Exception {
		List<DispatchBranch> branches = List.of(
			new DispatchBranch(1913, 203758, 0, List.of(PlayerClass.GLADIATOR, PlayerClass.TEMPLAR)),
			new DispatchBranch(1914, 203759, 1, List.of(PlayerClass.ASSASSIN, PlayerClass.RANGER)),
			new DispatchBranch(1915, 203760, 2, List.of(PlayerClass.SORCERER, PlayerClass.SPIRIT_MASTER)),
			new DispatchBranch(1916, 203761, 3, List.of(PlayerClass.CHANTER, PlayerClass.CLERIC)));

		for (DispatchBranch branch : branches) {
			QuestMetadata questMetadata = metadata(branch.questId());
			Map<Integer, QuestMetadata> metadata = Map.of(branch.questId(), questMetadata);
			QuestEvent event = new QuestEvent.TalkToNpc(branch.npcId(), 1002);

			for (PlayerClass permittedClass : branch.permittedClasses()) {
				Player permitted = player(10);
				setField(PlayerCommonData.class, permitted.getCommonData(), "playerClass", permittedClass);
				permitted.getQuestStateList().addQuest(1007,
					new QuestState(1007, QuestStatus.COMPLETE, 0, 1, null, branch.rewardMode(), null));
				assertTrue(port(permitted, metadata).snapshot(PLAYER_ID, branch.questId(), event).eligible());
			}

			Player wrongReward = player(10);
			setField(PlayerCommonData.class, wrongReward.getCommonData(), "playerClass",
				branch.permittedClasses().getFirst());
			wrongReward.getQuestStateList().addQuest(1007,
				new QuestState(1007, QuestStatus.COMPLETE, 0, 1, null,
					(branch.rewardMode() + 1) % branches.size(), null));
			assertRejected(port(wrongReward, metadata), branch.questId(), event, "START_CONDITION_REJECTED");

			Player wrongClass = player(10);
			setField(PlayerCommonData.class, wrongClass.getCommonData(), "playerClass", PlayerClass.GUNSLINGER);
			wrongClass.getQuestStateList().addQuest(1007,
				new QuestState(1007, QuestStatus.COMPLETE, 0, 1, null, branch.rewardMode(), null));
			assertRejected(port(wrongClass, metadata), branch.questId(), event, "CLASS_NOT_PERMITTED");
		}
	}

	@Test
	void abyssEntryFlightTestsAcceptEveryCompletedPrerequisiteRewardBranch() throws Exception {
		for (FlightTest flight : List.of(
			new FlightTest(1044, 1922, Race.ELYOS),
			new FlightTest(2042, 2947, Race.ASMODIANS))) {
			QuestMetadata target = metadata(flight.questId());
			QuestMetadata prerequisite = metadata(flight.prerequisiteId());
			Map<Integer, QuestMetadata> definitions = Map.of(
				flight.questId(), target, flight.prerequisiteId(), prerequisite);

			for (int rewardMode = 0; rewardMode < 3; rewardMode++) {
				int completedReward = rewardMode;
				Player player = player(45, flight.race());
				player.getQuestStateList().addQuest(flight.prerequisiteId(),
					new QuestState(flight.prerequisiteId(), QuestStatus.COMPLETE,
						0, 1, null, completedReward, null));
				PlayerQuestStartEligibilityPort eligibility = port(player, definitions);
				assertTrue(eligibility.snapshot(PLAYER_ID, flight.questId(), new QuestEvent.LevelUp()).eligible(),
					() -> "quest " + flight.questId() + " rejected reward " + completedReward + " on level-up");
				assertTrue(eligibility.snapshot(PLAYER_ID, flight.questId(), new QuestEvent.EnterWorld()).eligible(),
					() -> "quest " + flight.questId() + " rejected reward " + completedReward + " on login");
			}
		}
	}

	@Test
	void songOfBlessingAcceptsEveryAscensionRewardBranch() throws Exception {
		QuestMetadata songOfBlessing = metadata(2911);
		for (int rewardMode = 0; rewardMode <= 5; rewardMode++) {
			assertRewardBranchEligible(2911, songOfBlessing, 2009, rewardMode);
		}
		assertRewardBranchRejected(2911, songOfBlessing, 2009, 6);
	}

	private static void assertRewardBranchEligible(int questId, QuestMetadata metadata,
			int prerequisiteId, int rewardMode) throws Exception {
		Player player = player(65, Race.ASMODIANS);
		player.getQuestStateList().addQuest(prerequisiteId,
			new QuestState(prerequisiteId, QuestStatus.COMPLETE, 0, 1, null, rewardMode, null));
		assertTrue(port(player, Map.of(questId, metadata))
			.snapshot(PLAYER_ID, questId, new QuestEvent.LevelUp()).eligible(),
			() -> "quest " + questId + " rejected reward " + rewardMode);
	}

	private static void assertRewardBranchRejected(int questId, QuestMetadata metadata,
			int prerequisiteId, int rewardMode) throws Exception {
		Player player = player(65, Race.ASMODIANS);
		player.getQuestStateList().addQuest(prerequisiteId,
			new QuestState(prerequisiteId, QuestStatus.COMPLETE, 0, 1, null, rewardMode, null));
		assertRejected(port(player, Map.of(questId, metadata)), questId,
			new QuestEvent.LevelUp(), "START_CONDITION_REJECTED");
	}

	private record FlightTest(int questId, int prerequisiteId, Race race) {
	}

	private record DispatchBranch(int questId, int npcId, int rewardMode,
			List<PlayerClass> permittedClasses) {
	}

	private static TitleList titleListWith(int requiredTitleId) {
		return new TitleList() {
			@Override
			public boolean contains(int titleId) {
				return titleId == requiredTitleId;
			}
		};
	}

	@Test
	void activeNpcFactionRotationAllowsTheRotatedQuestToStart() throws Exception {
		// 35015 [Daily] Protecting Your Members: retained npc-faction quest (faction id 2, levels 40-50).
		QuestMetadata factionQuest = metadata(35015);
		Player factionPlayer = player(45);
		NpcFaction faction = new ObjenesisStd().newInstance(NpcFaction.class);
		setField(NpcFaction.class, faction, "id", 2);
		setField(NpcFaction.class, faction, "active", true);
		factionPlayer.setNpcFactions(new ActiveNpcFactions(faction));
		PlayerQuestStartEligibilityPort factionPort = port(factionPlayer, Map.of(35015, factionQuest));
		assertTrue(factionPort.snapshotNpcFactionRotation(PLAYER_ID, 35015, 2).eligible());
		setField(NpcFaction.class, faction, "questId", 35015);
		assertTrue(factionPort
			.snapshot(PLAYER_ID, 35015, new QuestEvent.LevelUp()).eligible());
	}

	@Test
	void pcAllMetadataAllowsBothPlayerRacesToStart() throws Exception {
		QuestMetadata metadata = metadata(80787);
		for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
			QuestStartEligibility result = port(player(1, race), Map.of(80787, metadata))
				.snapshot(PLAYER_ID, 80787, new QuestEvent.TalkToNpc(833671, 1002));

			assertTrue(result.eligible(), () -> race + " was rejected: " + result.reason());
		}
	}

	@Test
	void groupedStartConditionsUseAndInsideAGroupAndOrAcrossGroups() throws Exception {
		QuestMetadata grouped = metadataFromXml("""
			<metadata name="grouped" display-name-id="1" min-level="1" max-level="99" category="QUEST">
			  <races><race id="ELYOS"/></races>
			  <start-condition-groups>
			    <group><condition type="finished" quest-id="9001"/><condition type="acquired" quest-id="9002"/></group>
			    <group><condition type="finished" quest-id="9003"/></group>
			  </start-condition-groups>
			</metadata>
			""");
		Player player = player(10);
		player.getQuestStateList().addQuest(9003,
			new QuestState(9003, QuestStatus.COMPLETE, 0, 1, null, 0, null));

		assertTrue(port(player, Map.of(990001, grouped))
			.snapshot(PLAYER_ID, 990001, new QuestEvent.LevelUp()).eligible());
	}

	@Test
	void missingRequiredInventoryItemFailsClosedWithoutLegacyTemplateAccess() throws Exception {
		QuestMetadata metadata = metadataFromXml("""
			<metadata name="inventory" display-name-id="1" min-level="1" max-level="99" category="TASK">
			  <races><race id="ELYOS"/></races>
			  <inventory-items><item id="182400001" count="2"/></inventory-items>
			</metadata>
			""");
		QuestStartEligibility result = port(player(10), Map.of(987654, metadata))
			.snapshot(PLAYER_ID, 987654, new QuestEvent.LevelUp());

		assertFalse(result.eligible());
		assertEquals("REQUIRED_INVENTORY_ITEM_MISSING", result.reason());
	}

	@Test
	void npcFactionQuestStillRequiresTheActiveRotatedQuest() throws Exception {
		QuestMetadata metadata = metadata(35015);
		Player player = player(65);
		player.setNpcFactions(new MissingNpcFaction());

		QuestStartEligibility result = port(player, Map.of(35015, metadata))
			.snapshot(PLAYER_ID, 35015, new QuestEvent.LevelUp());

		assertFalse(result.eligible());
		assertEquals("NPC_FACTION_QUEST_NOT_ACTIVE", result.reason());
	}

	@Test
	void completedCharmedEventChildrenCanRestartThroughCanonicalRepeatMetadata() throws Exception {
		for (int questId : List.of(80034, 80035, 80036, 80037, 80038, 80039)) {
			int prerequisiteId = questId <= 80036 ? 80029 : 80032;
			Race race = questId <= 80036 ? Race.ELYOS : Race.ASMODIANS;
			QuestMetadata targetMetadata = metadata(questId);
			QuestMetadata prerequisiteMetadata = metadata(prerequisiteId);
			Player player = player(10, race);
			player.getQuestStateList().addQuest(prerequisiteId,
				new QuestState(prerequisiteId, QuestStatus.COMPLETE, 0, 1, null, 0, null));
			QuestState completed = new QuestState(questId, QuestStatus.COMPLETE, 0, 1, null, 0, null);
			player.getQuestStateList().addQuest(questId, completed);

			assertTrue(completed.canRepeat(targetMetadata), "quest " + questId + " should be repeatable");
			assertTrue(port(player, Map.of(questId, targetMetadata, prerequisiteId, prerequisiteMetadata))
				.snapshot(PLAYER_ID, questId, new QuestEvent.EventQuestRefresh()).eligible(),
				"quest " + questId + " refresh should pass canonical start eligibility");
		}
	}

	private static PlayerQuestStartEligibilityPort port(Player player, Map<Integer, QuestMetadata> metadata) {
		return new PlayerQuestStartEligibilityPort(playerId -> player, metadata::get, (questId, value) -> false);
	}

	private static void assertRejected(PlayerQuestStartEligibilityPort port, int questId, QuestEvent event,
			String reason) throws Exception {
		QuestStartEligibility result = port.snapshot(PLAYER_ID, questId, event);
		assertFalse(result.eligible());
		assertEquals(reason, result.reason());
	}

	private static QuestMetadata metadata(int questId) throws Exception {
		try (InputStream input = PlayerQuestStartEligibilityPortTest.class.getClassLoader().getResourceAsStream(
				"aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest " + questId);
			}
			return QuestDefinitionXmlCompiler.parse(input).metadata();
		}
	}

	private static QuestMetadata metadataFromXml(String metadata) {
		String xml = "<quest-definition id=\"990001\" version=\"1\">" + metadata
			+ "<nodes><node label=\"start\" status=\"START\"/></nodes>"
			+ "<transitions><transition source=\"start\" target=\"start\"><event><level-up/></event>"
			+ "</transition></transitions></quest-definition>";
		return QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).definition().metadata();
	}

	private static Player player(int level) throws Exception {
		return player(level, Race.ELYOS);
	}

	private static Player player(int level, Race race) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		PlayerCommonData common = new PlayerCommonData(PLAYER_ID);
		setField(PlayerCommonData.class, common, "level", level);
		setField(PlayerCommonData.class, common, "race", race);
		setField(PlayerCommonData.class, common, "playerClass", PlayerClass.WARRIOR);
		setField(PlayerCommonData.class, common, "gender", Gender.MALE);
		setField(Player.class, player, "playerCommonData", common);
		setField(Player.class, player, "questStateList", new QuestStateList());
		PlayerStorage inventory = new PlayerStorage(StorageType.CUBE);
		inventory.setOwner(player);
		setField(Player.class, player, "inventory", inventory);
		return player;
	}

	private static class MissingNpcFaction extends NpcFactions {
		private MissingNpcFaction() {
			super(null);
		}

		@Override
		public NpcFaction getNpcFactionById(int id) {
			return null;
		}
	}

	private static final class ActiveNpcFactions extends MissingNpcFaction {
		private final NpcFaction faction;

		private ActiveNpcFactions(NpcFaction faction) {
			this.faction = faction;
		}

		@Override
		public NpcFaction getNpcFactionById(int id) {
			return faction.getId() == id ? faction : null;
		}

		@Override
		public boolean canStartQuest(boolean mentor) {
			return true;
		}
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
