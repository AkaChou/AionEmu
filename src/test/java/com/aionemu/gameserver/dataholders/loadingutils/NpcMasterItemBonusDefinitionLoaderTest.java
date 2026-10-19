package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.npc.NpcMasterItemBonus;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

import jakarta.xml.bind.JAXBContext;

/**
 * 真端主人物品加成配置的回归闸门。
 * Regression gate for the retail master-item bonus configuration.
 */
class NpcMasterItemBonusDefinitionLoaderTest {

	private static final Path MASTER_ITEM_BONUS = Path.of(
		"src/main/resources/aion/definitions/compact/npc-master-item-bonus.xml");

	@TempDir
	Path tempDir;

	@Test
	void appliesMasterItemRatiosAndLimits() throws Exception {
		NpcData npcData = (NpcData) JAXBContext.newInstance(NpcData.class).createUnmarshaller().unmarshal(
			new StringReader("<npc_templates>"
				+ "<npc_template npc_id=\"201000\" level=\"24\" name_id=\"1\" npc_type=\"ATTACKABLE\">"
				+ "<stats/>"
				+ "</npc_template>"
				+ "</npc_templates>"));
		Path file = tempDir.resolve("npc-master-item-bonus.xml");
		Files.writeString(file, """
			<npc_master_item_bonuses>
			  <npc id="201000">
			    <stat name="physical_attack" ratio="50" limit="85"/>
			    <stat name="max_hp" ratio="20" limit="400"/>
			    <stat name="unknown_stat" ratio="50" limit="10"/>
			  </npc>
			</npc_master_item_bonuses>
			""");

		assertEquals(1, NpcMasterItemBonusDefinitionLoader.apply(file.toFile(), npcData));
		NpcMasterItemBonus bonus = npcData.getNpcTemplate(201000).getMasterItemBonus();
		assertNotNull(bonus);
		assertEquals(new NpcMasterItemBonus.Entry(50, 85), bonus.get(StatEnum.PHYSICAL_ATTACK));
		assertEquals(new NpcMasterItemBonus.Entry(20, 400), bonus.get(StatEnum.MAXHP));
		assertNull(bonus.get(StatEnum.MAGICAL_ATTACK));
	}

	@Test
	void retailMasterItemBonusOverlayMatchesSamples() throws Exception {
		int rows = 0;
		boolean saw201000 = false;
		boolean saw201001 = false;
		int currentId = -1;
		try (InputStream in = Files.newInputStream(MASTER_ITEM_BONUS)) {
			XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(in);
			while (reader.hasNext()) {
				int event = reader.next();
				if (event != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				String name = reader.getLocalName();
				if ("npc".equals(name)) {
					rows++;
					currentId = Integer.parseInt(reader.getAttributeValue(null, "id"));
					if (currentId == 201000) {
						saw201000 = true;
					} else if (currentId == 201001) {
						saw201001 = true;
					}
				} else if ("stat".equals(name) && currentId == 201000
					&& "physical_attack".equals(reader.getAttributeValue(null, "name"))) {
					assertEquals("50", reader.getAttributeValue(null, "ratio"));
					assertEquals("85", reader.getAttributeValue(null, "limit"));
				} else if ("stat".equals(name) && currentId == 201001
					&& "magical_skill_boost".equals(reader.getAttributeValue(null, "name"))) {
					assertEquals("30", reader.getAttributeValue(null, "ratio"));
					assertEquals("870", reader.getAttributeValue(null, "limit"));
				}
			}
			reader.close();
		}
		assertEquals(342, rows);
		assertTrue(saw201000);
		assertTrue(saw201001);
	}
}
