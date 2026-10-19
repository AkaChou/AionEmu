package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.npc.NpcAttackProfile;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

import jakarta.xml.bind.JAXBContext;

class NpcAttackDefinitionLoaderTest {

	private static final Path ATTACK_PROFILES = Path.of(
		"src/main/resources/aion/definitions/compact/npc-attack-profiles.xml");

	@TempDir
	Path tempDir;

	@Test
	void appliesRetailAttackProfiles() throws Exception {
		NpcData npcData = (NpcData) JAXBContext.newInstance(NpcData.class).createUnmarshaller().unmarshal(new StringReader("""
				<npc_templates>
				  <npc_template npc_id="200001" level="17" name_id="1" npc_type="ATTACKABLE" attack_range="2">
				    <stats/>
				  </npc_template>
				  <npc_template npc_id="204260" level="20" name_id="2" npc_type="ATTACKABLE" attack_range="2">
				    <stats/>
				  </npc_template>
				</npc_templates>
				"""));
		Path file = tempDir.resolve("npc-attack-profiles.xml");
		Files.writeString(file, """
				<npc_attack_profiles>
				  <npc id="200001">
				    <attack range="2" rate="1000" type="physical" attack_mod="100"/>
				    <attack range="32" rate="1000" type="physical" attack_mod="100"/>
				  </npc>
				  <npc id="204260">
				    <attack range="2" rate="1000" type="physical" attack_mod="100"/>
				    <attack range="26" rate="800" type="magical_Fire" attack_mod="150"/>
				  </npc>
				</npc_attack_profiles>
				""");

		assertEquals(2, NpcAttackDefinitionLoader.apply(file.toFile(), npcData));

		NpcTemplate melee = npcData.getNpcTemplate(200001);
		assertEquals(32f, melee.getMaxAttackRange());
		assertEquals(2, melee.getAttackProfiles().size());
		assertEquals(new NpcAttackProfile(2f, 1000, ItemAttackType.PHYSICAL, 100), melee.selectAttackProfile(1f));
		assertEquals(new NpcAttackProfile(32f, 1000, ItemAttackType.PHYSICAL, 100), melee.selectAttackProfile(20f));

		NpcTemplate magical = npcData.getNpcTemplate(204260);
		assertEquals(ItemAttackType.MAGICAL_FIRE, magical.selectAttackProfile(20f).type());
		assertEquals(150, magical.selectAttackProfile(20f).attackMod());
	}

	@Test
	void selectsFirstCoveringSegmentInRetailOrder() throws Exception {
		NpcData npcData = (NpcData) JAXBContext.newInstance(NpcData.class).createUnmarshaller().unmarshal(new StringReader("""
				<npc_templates>
				  <npc_template npc_id="231073" level="75" name_id="3" npc_type="ATTACKABLE" attack_range="10">
				    <stats/>
				  </npc_template>
				</npc_templates>
				"""));
		Path file = tempDir.resolve("npc-attack-profiles-order.xml");
		Files.writeString(file, """
				<npc_attack_profiles>
				  <npc id="231073">
				    <attack range="50" rate="1000" type="magical_Fire" attack_mod="100"/>
				    <attack range="10" rate="1000" type="physical" attack_mod="100"/>
				    <attack range="20" rate="1000" type="magical_Earth" attack_mod="150"/>
				  </npc>
				</npc_attack_profiles>
				""");

		assertEquals(1, NpcAttackDefinitionLoader.apply(file.toFile(), npcData));
		NpcTemplate template = npcData.getNpcTemplate(231073);

		// 50m 段在 retail 顺序中排第一，覆盖 5m/15m 时优先于更近的段。
		// The 50m segment is first in retail order and wins over closer segments at 5m and 15m.
		assertEquals(new NpcAttackProfile(50f, 1000, ItemAttackType.MAGICAL_FIRE, 100),
			template.selectAttackProfile(5f));
		assertEquals(new NpcAttackProfile(50f, 1000, ItemAttackType.MAGICAL_FIRE, 100),
			template.selectAttackProfile(15f));
		// 全部不覆盖时回退到最大射程段。 / Fall back to the longest-range segment when none covers.
		assertEquals(new NpcAttackProfile(50f, 1000, ItemAttackType.MAGICAL_FIRE, 100),
			template.selectAttackProfile(60f));
	}

	@Test
	void retailAttackProfilesCoverMultiAttackNpcs() throws Exception {
		int npcs = 0;
		int segments = 0;
		int currentId = -1;
		int attacksInCurrent = 0;
		boolean sawWormTest = false;
		boolean sawLycan = false;
		String lycanSecondType = null;
		String lycanSecondRange = null;
		int lycanSecondMod = -1;
		try (InputStream in = Files.newInputStream(ATTACK_PROFILES)) {
			XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(in);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				if ("npc".equals(reader.getLocalName())) {
					npcs++;
					currentId = Integer.parseInt(reader.getAttributeValue(null, "id"));
					attacksInCurrent = 0;
					if (currentId == 200001) {
						sawWormTest = true;
					}
					if (currentId == 204260) {
						sawLycan = true;
					}
				} else if ("attack".equals(reader.getLocalName())) {
					segments++;
					attacksInCurrent++;
					if (currentId == 204260 && attacksInCurrent == 2) {
						lycanSecondType = reader.getAttributeValue(null, "type");
						lycanSecondRange = reader.getAttributeValue(null, "range");
						lycanSecondMod = Integer.parseInt(reader.getAttributeValue(null, "attack_mod"));
					}
				}
			}
			reader.close();
		}
		assertEquals(393, npcs);
		assertEquals(787, segments);
		assertTrue(sawWormTest);
		assertTrue(sawLycan);
		assertEquals("magical_Fire", lycanSecondType);
		assertEquals("26", lycanSecondRange);
		assertEquals(100, lycanSecondMod);
	}
}
