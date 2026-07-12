package com.aionemu.gameserver.model.templates.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class NochsanaFortressGateTemplateTest {

	@Test
	void nochsanaFortressGateUsesDragonCastleDoorRaceInSourceAndRuntimeData() throws Exception {
		assertNochsanaFortressGateRace("src/main/resources/aion/data/static_data/npcs/npc_template.xml");
		assertNochsanaFortressGateRace("aion/data/static_data/npcs/npc_template.xml");
	}

	private static void assertNochsanaFortressGateRace(String path) throws Exception {
		Element template = npcTemplate(path, "256694");

		assertNotNull(template, "npc 256694 should exist in " + path);
		assertEquals("DRAGON_CASTLE_DOOR", template.getAttribute("race"), path);
	}

	private static Element npcTemplate(String path, String npcId) throws Exception {
		NodeList templates = xml(path).getElementsByTagName("npc_template");
		for (int i = 0; i < templates.getLength(); i++) {
			Element template = (Element) templates.item(i);
			if (npcId.equals(template.getAttribute("npc_id"))) {
				return template;
			}
		}
		return null;
	}

	private static Document xml(String path) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(Path.of(path).toFile());
	}
}
