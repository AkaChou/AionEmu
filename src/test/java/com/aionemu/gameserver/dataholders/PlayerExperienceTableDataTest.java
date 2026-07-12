package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

class PlayerExperienceTableDataTest {

	private static final Path EXPERIENCE_TABLE = Path.of(
			"src/main/resources/aion/data/static_data/player_experience_table.xml");

	@Test
	void playerExperienceTableUsesCumulativeFiveEightValuesThroughLevelOneHundred() throws Exception {
		List<Long> experience = readExperienceValues();

		assertEquals(100, experience.size());
		assertEquals(0L, experience.get(0));
		assertEquals(933228334012L, experience.get(76));
		assertEquals(10996964155892L, experience.get(84));
		assertEquals(241204263379477L, experience.get(99));
		for (int i = 1; i < experience.size(); i++) {
			assertTrue(experience.get(i) > experience.get(i - 1),
					"level " + (i + 1) + " should require more total exp");
		}
	}

	private List<Long> readExperienceValues() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		NodeList nodes = factory.newDocumentBuilder().parse(EXPERIENCE_TABLE.toFile()).getElementsByTagName("exp");
		List<Long> values = new ArrayList<>(nodes.getLength());
		for (int i = 0; i < nodes.getLength(); i++) {
			values.add(Long.parseLong(nodes.item(i).getTextContent().trim()));
		}
		return values;
	}
}
