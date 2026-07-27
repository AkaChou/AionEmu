package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.templates.stats.NpcStatsTemplate;

import jakarta.xml.bind.JAXBContext;

class NpcCombatDefinitionLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void appliesRetailNpcCombatData() throws Exception {
		NpcData npcData = (NpcData) JAXBContext.newInstance(NpcData.class).createUnmarshaller().unmarshal(new StringReader("""
				<npc_templates>
				  <npc_template npc_id="220305" level="66" name_id="1" npc_type="ATTACKABLE">
				    <stats/>
				  </npc_template>
				  <npc_template npc_id="216951" level="65" name_id="2" npc_type="ATTACKABLE">
				    <stats/>
				  </npc_template>
				  <npc_template npc_id="202697" level="1" name_id="3" npc_type="ATTACKABLE">
				    <stats/>
				  </npc_template>
				</npc_templates>
				"""));
		Path file = tempDir.resolve("npc-combat.xml");
		Files.writeString(file, """
				<npc_combat_data>
				  <npc id="220305" min_damage="2299" max_damage="3043" stat_ratio="1015"/>
				  <npc id="216951" min_damage="5466" max_damage="3345" stat_ratio="1000"/>
				  <npc id="202697" min_damage="0" max_damage="0" stat_ratio="1000"/>
				</npc_combat_data>
				""");

		assertEquals(3, NpcCombatDefinitionLoader.apply(file.toFile(), npcData));
		NpcStatsTemplate stats = npcData.getNpcTemplate(220305).getStatsTemplate();
		assertEquals(2299, stats.getMinDamage());
		assertEquals(3043, stats.getMaxDamage());
		assertEquals(1015, stats.getStatRatio());
		assertTrue(stats.hasRetailDamageRange());
		NpcStatsTemplate reversed = npcData.getNpcTemplate(216951).getStatsTemplate();
		assertEquals(5466, reversed.getMinDamage());
		assertEquals(3345, reversed.getMaxDamage());
		assertTrue(reversed.hasRetailDamageRange());
		NpcStatsTemplate zero = npcData.getNpcTemplate(202697).getStatsTemplate();
		assertEquals(0, zero.getMinDamage());
		assertEquals(0, zero.getMaxDamage());
		assertTrue(zero.hasRetailDamageRange());
	}

	@Test
	void retailCombatDataMatchesNpcTemplates() throws Exception {
		Set<Integer> templateIds = readIds(
				Path.of("src/main/resources/aion/data/static_data/npcs/npc_template.xml"), "npc_template", "npc_id", false);
		Set<Integer> combatIds = readUniqueIds(
				Path.of("src/main/resources/aion/definitions/compact/npc-combat.xml"), "npc", "id");

		assertEquals(41_455, combatIds.size());
		Set<Integer> missingIds = new HashSet<>(combatIds);
		missingIds.removeAll(templateIds);
		assertTrue(missingIds.isEmpty(), () -> "NPC combat data has no template: " + missingIds);
	}

	private static Set<Integer> readUniqueIds(Path file, String elementName, String attributeName) throws Exception {
		return readIds(file, elementName, attributeName, true);
	}

	private static Set<Integer> readIds(Path file, String elementName, String attributeName, boolean requireUnique)
			throws Exception {
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		Set<Integer> ids = new HashSet<>();
		try (InputStream stream = Files.newInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !reader.getLocalName().equals(elementName)) {
					continue;
				}
				int id = Integer.parseInt(reader.getAttributeValue(null, attributeName));
				boolean added = ids.add(id);
				assertTrue(added || !requireUnique, () -> file + " has duplicate NPC id " + id);
			}
			reader.close();
		}
		return ids;
	}
}
