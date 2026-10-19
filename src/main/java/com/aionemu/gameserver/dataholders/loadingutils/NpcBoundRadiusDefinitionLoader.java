package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

/**
 * 从真端投影加载 NPC 边界半径（全长约定）。
 * Loads retail-projected NPC bound radii (full-length convention).
 */
final class NpcBoundRadiusDefinitionLoader {

	/**
	 * 读取紧凑定义并覆盖对应 NPC 模板的边界半径。
	 * Reads the compact definition and overwrites the matching NPC templates' bound radii.
	 *
	 * @param file 定义文件 / definition file
	 * @param npcData NPC 数据容器 / NPC data holder
	 * @return 应用了边界半径的模板数 / number of templates that received a bound radius
	 */
	static int apply(File file, NpcData npcData) {
		int applied = 0;
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !"npc".equals(reader.getLocalName())) {
					continue;
				}
				NpcTemplate template = npcData.getNpcTemplate(attributeInt(reader, "id"));
				if (template == null) {
					continue;
				}
				template.setBoundRadius(new BoundRadius(
					attributeFloat(reader, "front"),
					attributeFloat(reader, "side"),
					attributeFloat(reader, "upper"),
					true));
				applied++;
			}
			reader.close();
			return applied;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load " + file.getPath(), e);
		}
	}

	private static int attributeInt(XMLStreamReader reader, String name) {
		return Integer.parseInt(attributeValue(reader, name));
	}

	private static float attributeFloat(XMLStreamReader reader, String name) {
		return Float.parseFloat(attributeValue(reader, name));
	}

	private static String attributeValue(XMLStreamReader reader, String name) {
		String value = reader.getAttributeValue(null, name);
		if (value == null) {
			throw new IllegalStateException("Missing NPC bound radius attribute: " + name);
		}
		return value;
	}
}
