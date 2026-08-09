package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.PlayerQuestStartEligibilityPort;

class EventServiceCanonicalMetadataTest {

	@Test
	void eventQuestUsesCanonicalRaceClassGenderAndGroupedStartConditions() throws Exception {
		QuestMetadata metadata = metadata("ELYOS");
		Player eligible = player(Race.ELYOS, PlayerClass.WARRIOR, Gender.MALE);
		eligible.getQuestStateList().addQuest(9003,
			new QuestState(9003, QuestStatus.COMPLETE, 0, 1, null, 0, null));

		assertTrue(matches(eligible, metadata));
		assertFalse(matches(player(Race.ASMODIANS, PlayerClass.WARRIOR, Gender.MALE), metadata));
		assertFalse(matches(player(Race.ELYOS, PlayerClass.MAGE, Gender.MALE), metadata));
		assertFalse(matches(player(Race.ELYOS, PlayerClass.WARRIOR, Gender.FEMALE), metadata));
	}

	@Test
	void pcAllEventQuestMatchesBothPlayerRaces() throws Exception {
		QuestMetadata metadata = metadata("PC_ALL");
		for (Race race : new Race[] { Race.ELYOS, Race.ASMODIANS }) {
			Player player = player(race, PlayerClass.WARRIOR, Gender.MALE);
			player.getQuestStateList().addQuest(9003,
				new QuestState(9003, QuestStatus.COMPLETE, 0, 1, null, 0, null));

			assertTrue(matches(player, metadata));
		}
	}

	private static boolean matches(Player player, QuestMetadata metadata) {
		Map<Integer, QuestMetadata> catalog = Map.of(990301, metadata);
		return EventService.matchesEventQuestMetadata(player, metadata,
			new PlayerQuestStartEligibilityPort(playerId -> player, catalog::get));
	}

	private static QuestMetadata metadata(String race) {
		String xml = """
			<quest-definition id="990301" version="1">
			  <metadata name="event" display-name-id="1" min-level="10" max-level="20" category="EVENT">
			    <races><race id="%s"/></races>
			    <classes><class id="WARRIOR"/></classes>
			    <gender id="MALE"/>
			    <start-condition-groups>
			      <group><condition type="finished" quest-id="9001"/><condition type="acquired" quest-id="9002"/></group>
			      <group><condition type="finished" quest-id="9003"/></group>
			    </start-condition-groups>
			  </metadata>
			  <nodes><node label="start"><project status="START"/></node></nodes>
			  <transitions><transition source="start" target="start"><event><level-up/></event></transition></transitions>
			</quest-definition>
			""".formatted(race);
		return QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).definition().metadata();
	}

	private static Player player(Race race, PlayerClass playerClass, Gender gender) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData common = new PlayerCommonData(7);
		setField(PlayerCommonData.class, common, "level", 15);
		setField(PlayerCommonData.class, common, "race", race);
		setField(PlayerCommonData.class, common, "playerClass", playerClass);
		setField(PlayerCommonData.class, common, "gender", gender);
		setField(Player.class, player, "playerCommonData", common);
		player.setQuestStateList(new QuestStateList());
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value)
			throws ReflectiveOperationException {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
