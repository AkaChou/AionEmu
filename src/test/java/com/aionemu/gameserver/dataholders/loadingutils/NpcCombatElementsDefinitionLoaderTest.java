package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

import jakarta.xml.bind.JAXBContext;

/**
 * 真端战斗元素覆盖层的回归闸门。
 * Regression gate for the retail combat-element overlay.
 */
class NpcCombatElementsDefinitionLoaderTest {

	private static final Path COMBAT_ELEMENTS = Path.of(
		"src/main/resources/aion/definitions/compact/npc-combat-elements.xml");

	@TempDir
	Path tempDir;

	@Test
	void appliesCombatElementsToTemplate() throws Exception {
		NpcData npcData = (NpcData) JAXBContext.newInstance(NpcData.class).createUnmarshaller().unmarshal(
			new StringReader("<npc_templates>"
				+ "<npc_template npc_id=\"201003\" level=\"65\" name_id=\"1\" npc_type=\"ATTACKABLE\">"
				+ "<stats/>"
				+ "</npc_template>"
				+ "</npc_templates>"));
		Path file = tempDir.resolve("npc-combat-elements.xml");
		Files.writeString(file, """
			<npc_combat_elements>
			  <npc id="201003" hit_count="2" dp="200" magical_hit_accuracy="408"
			       physical_critical_reduce_rate="127" elemental_defend_fire="56"
			       limit_attribute="Type_A" elemental_immunity="erfire,erWater">
			    <abnormal_resist name="Stat_ArAll" value="100"/>
			    <abnormal_resist name="sleep,bleed" value="50"/>
			    <abnormal_penetration name="Stat_ArStunLike" value="300"/>
			    <abnormal_penetration name="sleep" value="20"/>
			  </npc>
			</npc_combat_elements>
			""");

		assertEquals(1, NpcCombatElementsDefinitionLoader.apply(file.toFile(), npcData));
		NpcTemplate template = npcData.getNpcTemplate(201003);
		assertEquals(2, template.getHitCount());
		assertEquals(200, template.getCombatStatBase(StatEnum.MAXDP).intValue());
		assertEquals(408, template.getCombatStatBase(StatEnum.MAGICAL_ACCURACY).intValue());
		assertEquals(127, template.getCombatStatBase(StatEnum.PHYSICAL_CRITICAL_REDUCE_RATE).intValue());
		assertEquals(56, template.getCombatStatBase(StatEnum.FIRE_RESISTANCE).intValue());
		assertEquals(100, template.getCombatStatBase(StatEnum.ABNORMAL_RESISTANCE_ALL).intValue());
		assertEquals(50, template.getCombatStatBase(StatEnum.SLEEP_RESISTANCE).intValue());
		assertEquals(50, template.getCombatStatBase(StatEnum.BLEED_RESISTANCE).intValue());
		assertEquals(300, template.getCombatStatBase(StatEnum.STUN_RESISTANCE_PENETRATION).intValue());
		assertEquals(300, template.getCombatStatBase(StatEnum.CHARM_RESISTANCE_PENETRATION).intValue());
		assertEquals(20, template.getCombatStatBase(StatEnum.SLEEP_RESISTANCE_PENETRATION).intValue());
		assertEquals(StatEnum.PVE_ATTACK_RATIO_TYPE_A, template.getLimitAttributeStat());
		assertTrue(template.isElementallyImmune(SkillElement.FIRE));
		assertTrue(template.isElementallyImmune(SkillElement.WATER));
		assertFalse(template.isElementallyImmune(SkillElement.WIND));
		assertNull(template.getCombatStatBase(StatEnum.WIND_RESISTANCE));
	}

	@Test
	void unknownTokensAndMissingIdsAreIgnored() throws Exception {
		NpcData npcData = (NpcData) JAXBContext.newInstance(NpcData.class).createUnmarshaller().unmarshal(
			new StringReader("<npc_templates>"
				+ "<npc_template npc_id=\"201004\" level=\"65\" name_id=\"1\" npc_type=\"ATTACKABLE\">"
				+ "<stats/>"
				+ "</npc_template>"
				+ "</npc_templates>"));
		Path file = tempDir.resolve("npc-combat-elements.xml");
		Files.writeString(file, """
			<npc_combat_elements>
			  <npc id="999999" dp="1"/>
			  <npc id="201004" elemental_immunity="Sleep">
			    <abnormal_resist name="unknown_abnormal" value="10"/>
			    <abnormal_penetration name="unknown_abnormal" value="10"/>
			  </npc>
			</npc_combat_elements>
			""");

		assertEquals(1, NpcCombatElementsDefinitionLoader.apply(file.toFile(), npcData));
		NpcTemplate template = npcData.getNpcTemplate(201004);
		assertEquals(1, template.getHitCount());
		assertNull(template.getCombatStatBase(StatEnum.MAXDP));
		assertFalse(template.isElementallyImmune(SkillElement.FIRE));
	}

	@Test
	void retailCombatElementsOverlayMatchesSamples() throws Exception {
		int rows = 0;
		boolean saw200000 = false;
		boolean saw201000 = false;
		int currentId = -1;
		try (InputStream in = Files.newInputStream(COMBAT_ELEMENTS)) {
			XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(in);
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					String name = reader.getLocalName();
					if ("npc".equals(name)) {
						rows++;
						currentId = Integer.parseInt(reader.getAttributeValue(null, "id"));
						if (currentId == 200000) {
							saw200000 = true;
							assertEquals("200", reader.getAttributeValue(null, "dp"));
							assertEquals("384", reader.getAttributeValue(null, "magical_hit_accuracy"));
							assertEquals("100", reader.getAttributeValue(null, "cancel_level"));
							assertEquals("120", reader.getAttributeValue(null, "elemental_defend_fire"));
						} else if (currentId == 201000) {
							saw201000 = true;
							assertEquals("50", reader.getAttributeValue(null, "dp"));
						}
					} else if ("abnormal_resist".equals(name) && currentId == 200000) {
						String token = reader.getAttributeValue(null, "name");
						if ("sleep".equals(token)) {
							assertEquals("100", reader.getAttributeValue(null, "value"));
						} else if ("bleed".equals(token)) {
							assertEquals("50", reader.getAttributeValue(null, "value"));
						}
					}
				}
			}
			reader.close();
		}
		assertEquals(87_169, rows);
		assertTrue(saw200000);
		assertTrue(saw201000);
	}
}
