package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.npc.NpcAttackProfile;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

/**
 * 从真端投影加载 NPC 多段攻击档案。
 * Loads retail-projected NPC multi-segment attack profiles.
 */
final class NpcAttackDefinitionLoader {

	/**
	 * 读取紧凑定义并挂到对应 NPC 模板上。
	 * Reads the compact definition and attaches profiles to the matching NPC templates.
	 *
	 * @param file 定义文件 / definition file
	 * @param npcData NPC 数据容器 / NPC data holder
	 * @return 应用了多段攻击的模板数 / number of templates that received profiles
	 */
	static int apply(File file, NpcData npcData) {
		Set<Integer> applied = new HashSet<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			NpcTemplate current = null;
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				switch (reader.getLocalName()) {
					case "npc" -> current = npcData.getNpcTemplate(attribute(reader, "id"));
					case "attack" -> {
						if (current == null) {
							throw new IllegalStateException("NPC attack profile without npc: " + file.getPath());
						}
						List<NpcAttackProfile> profiles = current.getAttackProfiles();
						if (profiles == null) {
							profiles = new ArrayList<>();
							current.setAttackProfiles(profiles);
						}
						profiles.add(new NpcAttackProfile(
							Float.parseFloat(attributeValue(reader, "range")),
							attribute(reader, "rate"),
							attackType(attributeValue(reader, "type")),
							attribute(reader, "attack_mod")));
						applied.add(current.getTemplateId());
					}
					default -> {
					}
				}
			}
			reader.close();
			return applied.size();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load " + file.getPath(), e);
		}
	}

	private static ItemAttackType attackType(String value) {
		String normalized = value.toLowerCase(Locale.ROOT);
		if (normalized.startsWith("magical_")) {
			normalized = normalized.substring("magical_".length());
		}
		return switch (normalized) {
			case "physical" -> ItemAttackType.PHYSICAL;
			case "fire" -> ItemAttackType.MAGICAL_FIRE;
			case "water" -> ItemAttackType.MAGICAL_WATER;
			case "air", "wind" -> ItemAttackType.MAGICAL_WIND;
			case "earth" -> ItemAttackType.MAGICAL_EARTH;
			default -> throw new IllegalArgumentException("Unknown NPC attack type: " + value);
		};
	}

	private static int attribute(XMLStreamReader reader, String name) {
		return Integer.parseInt(attributeValue(reader, name));
	}

	private static String attributeValue(XMLStreamReader reader, String name) {
		String value = reader.getAttributeValue(null, name);
		if (value == null) {
			throw new IllegalStateException("Missing NPC attack attribute: " + name);
		}
		return value;
	}
}
