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
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

import jakarta.xml.bind.JAXBContext;

/**
 * 真端边界半径覆盖层的回归闸门。
 * Regression gate for the retail bound-radius overlay.
 */
class NpcBoundRadiusDefinitionLoaderTest {

	private static final Path BOUND_RADII = Path.of(
		"src/main/resources/aion/definitions/compact/npc-bound-radius.xml");

	@TempDir
	Path tempDir;

	@Test
	void appliesFullLengthBoundRadius() throws Exception {
		NpcData npcData = (NpcData) JAXBContext.newInstance(NpcData.class).createUnmarshaller().unmarshal(
			new StringReader("<npc_templates>"
				+ "<npc_template npc_id=\"201003\" level=\"65\" name_id=\"1\" npc_type=\"ATTACKABLE\" attack_range=\"2\">"
				+ "<stats/>"
				+ "</npc_template>"
				+ "</npc_templates>"));
		Path file = tempDir.resolve("npc-bound-radius.xml");
		Files.writeString(file, "<npc_bound_radii>"
			+ "<npc id=\"201003\" front=\"0.5000\" side=\"0.5000\" upper=\"1.3000\"/>"
			+ "</npc_bound_radii>");

		assertEquals(1, NpcBoundRadiusDefinitionLoader.apply(file.toFile(), npcData));
		NpcTemplate template = npcData.getNpcTemplate(201003);
		BoundRadius bound = template.getBoundRadius();
		assertTrue(bound.isFullLength());
		assertEquals(0.5f, bound.getFront(), 1e-6);
		assertEquals(0.5f, bound.getSide(), 1e-6);
		assertEquals(1.3f, bound.getUpper(), 1e-6);
		assertEquals(0.25f, bound.getCollision(), 1e-6);
	}

	@Test
	void retailBoundRadiusOverlayCoversTemplates() throws Exception {
		int rows = 0;
		boolean sawSkillZone = false;
		boolean sawHalfValue = false;
		boolean sawScaled = false;
		boolean sawOriginal = false;
		try (InputStream in = Files.newInputStream(BOUND_RADII)) {
			XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(in);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !"npc".equals(reader.getLocalName())) {
					continue;
				}
				rows++;
				int id = Integer.parseInt(reader.getAttributeValue(null, "id"));
				if (id == 200000) {
					sawSkillZone = true;
					assertEquals("0.1200", reader.getAttributeValue(null, "front"));
					assertEquals("0.3456", reader.getAttributeValue(null, "side"));
					assertEquals("0.3000", reader.getAttributeValue(null, "upper"));
				} else if (id == 201003) {
					sawHalfValue = true;
					assertEquals("0.5000", reader.getAttributeValue(null, "front"));
					assertEquals("0.5000", reader.getAttributeValue(null, "side"));
					assertEquals("1.3000", reader.getAttributeValue(null, "upper"));
				} else if (id == 202514) {
					sawScaled = true;
					assertEquals("1.8000", reader.getAttributeValue(null, "front"));
					assertEquals("1.0800", reader.getAttributeValue(null, "side"));
					assertEquals("1.3200", reader.getAttributeValue(null, "upper"));
				} else if (id == 833817) {
					sawOriginal = true;
					assertEquals("0.5000", reader.getAttributeValue(null, "front"));
					assertEquals("0.7000", reader.getAttributeValue(null, "side"));
					assertEquals("2.0000", reader.getAttributeValue(null, "upper"));
				}
			}
			reader.close();
		}
		assertEquals(87_705, rows);
		assertTrue(sawSkillZone);
		assertTrue(sawHalfValue);
		assertTrue(sawScaled);
		assertTrue(sawOriginal);
	}
}
