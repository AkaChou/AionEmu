package com.aionemu.gameserver.dataholders.loadingutils;

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
}
