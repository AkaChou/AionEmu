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
	void questSchemaAcceptsMultiplePermittedRaces() throws Exception {
		var schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
			.newSchema(Path.of("src/main/resources/aion/data/static_data/quest_data/quest_data.xsd").toFile());

		schema.newValidator().validate(new StreamSource(new StringReader(TWO_RACE_QUEST)));
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
