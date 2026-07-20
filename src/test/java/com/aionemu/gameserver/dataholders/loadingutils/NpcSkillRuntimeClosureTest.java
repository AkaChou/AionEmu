package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;

class NpcSkillRuntimeClosureTest {

	private static final Path SKILLS_DIRECTORY = Path.of("src/main/resources/aion/definitions/compact/skills");

	@Test
	void allResolvedNpcSkillsExistInRuntimeSkillData() throws Exception {
		var skillData = SkillDefinitionLoader.load(SKILLS_DIRECTORY.toFile());
		Set<Integer> missingSkillIds = new TreeSet<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (InputStream stream = Files.newInputStream(SKILLS_DIRECTORY.resolve("npc-skills.xml"))) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !reader.getLocalName().equals("skill")) {
					continue;
				}
				String value = reader.getAttributeValue(null, "id");
				if (value != null && skillData.getSkillTemplate(Integer.parseInt(value)) == null) {
					missingSkillIds.add(Integer.parseInt(value));
				}
			}
			reader.close();
		}

		assertTrue(missingSkillIds.isEmpty(), () -> "NPC skills missing from SkillData: " + missingSkillIds);
	}

	@Test
	void preservesRetailNpcSkillOrphanSlots() throws Exception {
		Set<String> orphanGroups = new TreeSet<>();
		Set<Integer> affectedNpcIds = new TreeSet<>();
		int orphanSlots = 0;
		String groupId = null;
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (InputStream stream = Files.newInputStream(SKILLS_DIRECTORY.resolve("npc-skills.xml"))) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				if (reader.getLocalName().equals("group")) {
					groupId = reader.getAttributeValue(null, "id");
				} else if (reader.getLocalName().equals("skill") && reader.getAttributeValue(null, "id") == null) {
					orphanSlots++;
					orphanGroups.add(groupId);
				} else if (reader.getLocalName().equals("assign")
					&& orphanGroups.contains(reader.getAttributeValue(null, "group"))) {
					for (String npcId : reader.getAttributeValue(null, "npc_ids").trim().split("\\s+")) {
						affectedNpcIds.add(Integer.parseInt(npcId));
					}
				}
			}
			reader.close();
		}

		assertEquals(317, orphanSlots);
		assertEquals(84, orphanGroups.size());
		assertEquals(156, affectedNpcIds.size());
	}
}
