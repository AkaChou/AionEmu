package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.gameserver.dataholders.NpcPathBehaviorData;
import com.aionemu.gameserver.dataholders.NpcPathBehaviorData.Behavior;
import com.aionemu.gameserver.dataholders.NpcPathBehaviorData.PathfindFailReaction;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 从 XML 加载 NPC 寻路行为定义（最大追击时间、寻路失败反应、返回方式等）。
 * Loads NPC path-finding behavior definitions (max chase time, pathfind-fail reaction, return mode, etc.) from XML.
 */
final class NpcPathBehaviorDefinitionLoader {

	static NpcPathBehaviorData load(File file) {
		Map<Integer, Behavior> behaviors = new HashMap<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT || !reader.getLocalName().equals("npc")) {
					continue;
				}
				int id = Integer.parseInt(attribute(reader, "id"));
				Behavior behavior = new Behavior(attribute(reader, "max_chase_time"),
					PathfindFailReaction.valueOf(attribute(reader, "react_to_pathfind_fail", "return_to_sp")
						.toUpperCase(Locale.ROOT)), attribute(reader, "move_type_return", "walk"),
					Integer.parseInt(attribute(reader, "move_speed_return", "150")),
					Integer.parseInt(attribute(reader, "decrease_sensory_range_return", "50")));
				if (behaviors.put(id, behavior) != null) {
					throw new IllegalStateException("Duplicate NPC path behavior: " + id);
				}
			}
			reader.close();
			return new NpcPathBehaviorData(behaviors);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load NPC path behavior from " + file.getPath(), e);
		}
	}

	private static String attribute(XMLStreamReader reader, String name) {
		return reader.getAttributeValue(null, name);
	}

	private static String attribute(XMLStreamReader reader, String name, String defaultValue) {
		String value = attribute(reader, name);
		return value == null || value.isBlank() ? defaultValue : value;
	}
}
