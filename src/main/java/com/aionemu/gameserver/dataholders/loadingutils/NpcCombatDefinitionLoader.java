package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.stats.NpcStatsTemplate;

/**
 * 从 XML 加载 NPC 战斗属性定义（伤害范围、属性比率、属性削弱上限）。
 * Loads NPC combat attribute definitions (damage range, stat ratios, attribute reduction caps) from XML.
 */
final class NpcCombatDefinitionLoader {

	static int apply(File file, NpcData npcData) {
		int applied = 0;
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !reader.getLocalName().equals("npc")) {
					continue;
				}
				NpcTemplate template = npcData.getNpcTemplate(attribute(reader, "id"));
				if (template == null || template.getStatsTemplate() == null) {
					continue;
				}
				NpcStatsTemplate stats = template.getStatsTemplate();
				stats.setDamageRange(attribute(reader, "min_damage"), attribute(reader, "max_damage"));
				stats.setStatRatio(attribute(reader, "stat_ratio"));
				stats.setLimitAttributeReduceValue(attribute(reader, "limit_attribute_reduce_value", 0));
				applied++;
			}
			reader.close();
			return applied;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load " + file.getPath(), e);
		}
	}

	private static int attribute(XMLStreamReader reader, String name) {
		String value = reader.getAttributeValue(null, name);
		if (value == null) {
			throw new IllegalStateException("Missing NPC combat attribute: " + name);
		}
		return Integer.parseInt(value);
	}

	private static int attribute(XMLStreamReader reader, String name, int defaultValue) {
		String value = reader.getAttributeValue(null, name);
		return value == null ? defaultValue : Integer.parseInt(value);
	}
}
