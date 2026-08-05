package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMetadataFieldMappingTest {
	@Test
	void currentQuestDataFieldsHaveAnExplicitCanonicalDestination() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		Document document;
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_data/quest_data.xml")) {
			document = factory.newDocumentBuilder().parse(input);
		}
		assertEquals("quests", document.getDocumentElement().getTagName());
		NodeList quests = document.getDocumentElement().getElementsByTagName("quest");
		assertEquals(6429, quests.getLength());

		Set<String> attributes = new HashSet<>();
		Set<String> elements = new HashSet<>();
		for (int i = 0; i < quests.getLength(); i++) {
			Element quest = (Element) quests.item(i);
			for (int a = 0; a < quest.getAttributes().getLength(); a++) {
				attributes.add(quest.getAttributes().item(a).getNodeName());
			}
			NodeList children = quest.getChildNodes();
			for (int c = 0; c < children.getLength(); c++) {
				Node child = children.item(c);
				if (child instanceof Element element) {
					elements.add(element.getTagName());
				}
			}
		}

		MapAssertions.assertMapped(attributes, "quest attribute");
		MapAssertions.assertMapped(elements, "quest element");
		assertTrue(QuestMetadataFieldMapping.mapping().size() >= attributes.size() + elements.size());
	}

	private static final class MapAssertions {
		private static void assertMapped(Set<String> names, String kind) {
			for (String name : names) {
				assertTrue(QuestMetadataFieldMapping.mapping().containsKey(name),
					() -> kind + " is unmapped: " + name);
			}
		}
	}
}
