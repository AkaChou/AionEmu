package com.aionemu.gameserver.dataholders;

import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;

import com.aionemu.gameserver.model.templates.quest.QuestItems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRandomRewardsDataTest {
	private static final Path DATA = Path.of(
		"src/main/resources/aion/data/static_data/quest_random_rewards.xml");

	@Test
	void retailPoolUsesWeightedBoundariesAndReturnsTheConfiguredCounts() throws Exception {
		QuestRandomRewardsData data = load();

		assertTrue(data.containsPool(18505));
		assertItem(data.draw(18505, total -> 1), 182005205, 1);
		assertItem(data.draw(18505, total -> 700000), 182005205, 1);
		assertItem(data.draw(18505, total -> 700001), 186000469, 210);
		assertItem(data.draw(18505, total -> 1000000), 186000469, 210);
	}

	@Test
	void unknownPoolAndOutOfRangeRollFailClosed() throws Exception {
		QuestRandomRewardsData data = load();

		assertThrows(IllegalArgumentException.class, () -> data.draw(99999));
		assertThrows(IllegalArgumentException.class, () -> data.draw(18505, total -> 0));
		assertThrows(IllegalArgumentException.class, () -> data.draw(18505, total -> total + 1));
	}

	@Test
	void retailPoolFileValidatesAgainstItsSchema() throws Exception {
		var schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
			.newSchema(Path.of("src/main/resources/aion/data/static_data/quest_random_rewards.xsd").toFile());

		schema.newValidator().validate(new StreamSource(DATA.toFile()));
	}

	private static QuestRandomRewardsData load() throws Exception {
		return (QuestRandomRewardsData) JAXBContext.newInstance(QuestRandomRewardsData.class)
			.createUnmarshaller().unmarshal(DATA.toFile());
	}

	private static void assertItem(QuestItems item, int itemId, int count) {
		assertEquals(itemId, item.getItemId());
		assertEquals(count, item.getCount());
	}
}
