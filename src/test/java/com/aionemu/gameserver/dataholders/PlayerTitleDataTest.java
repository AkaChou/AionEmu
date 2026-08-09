package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class PlayerTitleDataTest {

	private static final Path PLAYER_TITLES = Path.of(
			"src/main/resources/aion/data/static_data/player_titles.xml");

	@Test
	void titleNameIdsUseDescriptionIdWireEncoding() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		NodeList titles = factory.newDocumentBuilder().parse(PLAYER_TITLES.toFile()).getElementsByTagName("title");
		int newDaevaMemoriesNameId = 0;

		for (int i = 0; i < titles.getLength(); i++) {
			Element title = (Element) titles.item(i);
			int titleId = Integer.parseInt(title.getAttribute("id"));
			int nameId = Integer.parseInt(title.getAttribute("nameId"));
			assertTrue(nameId >= 2_000_000, () -> "title " + titleId + " uses an unencoded nameId " + nameId);
			assertEquals(1, nameId & 1, () -> "title " + titleId + " nameId must use stringId * 2 + 1");
			if (titleId == 303) {
				newDaevaMemoriesNameId = nameId;
			}
		}

		assertEquals(3_604_065, newDaevaMemoriesNameId);
	}
}
