package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class KromedesTrialStaticDoorDataTest {

	private static final Path STATIC_DOORS = Path.of(
		"src/main/resources/aion/data/static_data/staticdoors/staticdoor_templates.xml");

	@Test
	void keyLockedDoorsUseTheKromedesTrialKeysAndStartClickable() throws Exception {
		Document document = parseStaticDoors();

		assertDoor(document, "2", "185000098");
		assertDoor(document, "326", "185000099");
		assertDoor(document, "325", "185000100");
	}

	private static void assertDoor(Document document, String doorId, String keyId) throws Exception {
		String expression = "/staticdoor_templates/world[@world='300230000']/staticdoor[@doorid='"
			+ doorId + "']";
		Element door = (Element) XPathFactory.newInstance().newXPath()
			.evaluate(expression, document, XPathConstants.NODE);

		assertNotNull(door, "door " + doorId + " should exist");
		assertEquals(keyId, door.getAttribute("keyid"), "door " + doorId + " key");
		assertEquals("0x2", door.getAttribute("state"), "door " + doorId + " initial state");
	}

	private static Document parseStaticDoors() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(STATIC_DOORS.toFile());
	}
}
