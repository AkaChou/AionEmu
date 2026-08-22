package com.aionemu.gameserver.model.templates.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class NochsanaFortressGateTemplateTest {

	@Test
	void nochsanaFortressGateUsesDragonCastleDoorRaceInSourceAndRuntimeData() throws Exception {
		assertNochsanaFortressGateRace("src/main/resources/aion/data/static_data/npcs");
		assertNochsanaFortressGateRace("target/classes/aion/data/static_data/npcs");
	}

	private static void assertNochsanaFortressGateRace(String path) throws Exception {
		Element template = npcTemplate(path, "256694");

		assertNotNull(template, "npc 256694 should exist in " + path);
		assertEquals("DRAGON_CASTLE_DOOR", template.getAttribute("race"), path);
	}

	private static Element npcTemplate(String path, String npcId) throws Exception {
		Path source = Path.of(path);
		if (Files.isDirectory(source)) {
			try (var paths = Files.list(source)) {
				for (Path shard : paths.filter(file -> file.getFileName().toString()
					.matches("npc_template_\\d+_\\d+\\.xml")).sorted().toList()) {
					Element template = npcTemplate(shard, npcId);
					if (template != null) {
						return template;
					}
				}
			}
			return null;
		}
		return npcTemplate(source, npcId);
	}

	private static Element npcTemplate(Path path, String npcId) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		NodeList templates = factory.newDocumentBuilder().parse(path.toFile()).getElementsByTagName("npc_template");
		for (int i = 0; i < templates.getLength(); i++) {
			Element template = (Element) templates.item(i);
			if (npcId.equals(template.getAttribute("npc_id"))) {
				return template;
			}
		}
		return null;
	}
}
