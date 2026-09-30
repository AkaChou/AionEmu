package com.aionemu.gameserver.model.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;

import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.model.Race;
import org.junit.jupiter.api.Test;

class QuestTemplateRacePermittedTest {

	private static final String TWO_RACE_QUEST = """
		<quests>
			<quest id="1315" race_permitted="ELYOS ASMODIANS"/>
		</quests>
		""";

	@Test
	void fromMetadataAcceptsMultiplePermittedRaces() {
		var meta = new com.aionemu.gameserver.questEngine.definition.QuestMetadata(
			"Q1315", 0, 10, 50, java.util.Set.of("ELYOS", "ASMODIANS"), "QUEST", com.aionemu.gameserver.questEngine.definition.RepeatPolicy.once(),
			java.util.Set.of(), List.of(), List.of(), List.of());
		QuestTemplate quest = QuestTemplate.fromMetadata(1315, meta);
		assertTrue(quest.isRacePermitted(Race.ELYOS));
		assertTrue(quest.isRacePermitted(Race.ASMODIANS));
		assertFalse(quest.isRacePermitted(Race.NPC));
	}

	@Test
	void jaxbLoadsMultiplePermittedRacesAndAppliesThemAsAlternatives() throws Exception {
		QuestsData quests = (QuestsData) JAXBContext.newInstance(QuestsData.class)
			.createUnmarshaller().unmarshal(new StringReader(TWO_RACE_QUEST));
		QuestTemplate quest = quests.getQuestById(1315);

		assertEquals(List.of(Race.ELYOS, Race.ASMODIANS), quest.getRacePermitted());
		assertTrue(quest.isRacePermitted(Race.ELYOS));
		assertTrue(quest.isRacePermitted(Race.ASMODIANS));
		assertFalse(quest.isRacePermitted(Race.NPC));
	}

	@Test
	void missingRestrictionAndPcAllPermitEitherPlayerRace() {
		QuestTemplate unrestricted = new QuestTemplate();
		QuestTemplate allPlayers = new QuestTemplate();
		allPlayers.getRacePermitted().add(Race.PC_ALL);

		assertTrue(unrestricted.isRacePermitted(Race.ELYOS));
		assertTrue(unrestricted.isRacePermitted(Race.ASMODIANS));
		assertTrue(allPlayers.isRacePermitted(Race.ELYOS));
		assertTrue(allPlayers.isRacePermitted(Race.ASMODIANS));
	}
}
