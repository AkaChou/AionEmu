package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.npc.NpcMasterItemBonus;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

/**
 * 从真端投影加载 NPC 主人物品加成配置。
 * Loads the retail-projected NPC master-item bonus configuration.
 * <p>真端为召唤物模板保存 28 组 {@code master_item_*_ratio/limit}；本地运行时在召唤物
 * 属性容器读取主人装备加成时使用这些比例与上限。
 * Retail stores 28 {@code master_item_*_ratio/limit} pairs on summon templates; the local
 * summon stat containers use those ratios and limits when reading the master's equipment bonus.
 */
final class NpcMasterItemBonusDefinitionLoader {

	/**
	 * 读取紧凑定义并挂载到对应 NPC 模板。
	 * Reads the compact definition and attaches it to the matching NPC templates.
	 *
	 * @param file    定义文件 / definition file
	 * @param npcData NPC 数据容器 / NPC data holder
	 * @return 应用了配置的模板数 / number of templates that received a configuration
	 */
	static int apply(File file, NpcData npcData) {
		int applied = 0;
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			NpcMasterItemBonus current = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					switch (reader.getLocalName()) {
						case "npc" -> {
							NpcTemplate template = npcData.getNpcTemplate(attributeInt(reader, "id"));
							if (template == null) {
								current = null;
								continue;
							}
							current = new NpcMasterItemBonus();
							template.setMasterItemBonus(current);
							applied++;
						}
						case "stat" -> {
							if (current != null) {
								StatEnum stat = stat(reader.getAttributeValue(null, "name"));
								if (stat != null) {
									current.put(stat, attributeInt(reader, "ratio"), attributeInt(reader, "limit"));
								}
							}
						}
						default -> {
						}
					}
				} else if (event == XMLStreamConstants.END_ELEMENT && "npc".equals(reader.getLocalName())) {
					current = null;
				}
			}
			reader.close();
			return applied;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load " + file.getPath(), e);
		}
	}

	/**
	 * 将真端 master_item 属性名映射为本地召唤物侧属性。
	 * Maps a retail master_item stat name to the local summon-side stat.
	 *
	 * @param name 真端属性名 / retail stat name
	 * @return 本地属性；未知名称时为 {@code null} / local stat, or {@code null}
	 */
	static StatEnum stat(String name) {
		if (name == null) {
			return null;
		}
		return switch (name.toLowerCase(Locale.ROOT)) {
			case "max_hp" -> StatEnum.MAXHP;
			case "hpregen" -> StatEnum.REGEN_HP;
			case "physical_attack" -> StatEnum.PHYSICAL_ATTACK;
			case "critical" -> StatEnum.PHYSICAL_CRITICAL;
			case "hit_accuracy" -> StatEnum.PHYSICAL_ACCURACY;
			case "dodge" -> StatEnum.EVASION;
			case "parry" -> StatEnum.PARRY;
			case "block" -> StatEnum.BLOCK;
			case "physical_defend" -> StatEnum.PHYSICAL_DEFENSE;
			case "magical_attack" -> StatEnum.MAGICAL_ATTACK;
			case "magical_defend" -> StatEnum.MAGICAL_DEFEND;
			case "magical_skill_boost" -> StatEnum.BOOST_MAGICAL_SKILL;
			case "magical_skill_boost_resist" -> StatEnum.MAGIC_SKILL_BOOST_RESIST;
			case "magical_critical" -> StatEnum.MAGICAL_CRITICAL;
			case "magical_hit_accuracy" -> StatEnum.MAGICAL_ACCURACY;
			case "magical_resist" -> StatEnum.MAGICAL_RESIST;
			case "elemental_defend_light" -> StatEnum.ELEMENTAL_RESISTANCE_LIGHT;
			case "elemental_defend_dark" -> StatEnum.ELEMENTAL_RESISTANCE_DARK;
			case "elemental_defend_fire" -> StatEnum.FIRE_RESISTANCE;
			case "elemental_defend_water" -> StatEnum.WATER_RESISTANCE;
			case "elemental_defend_air" -> StatEnum.WIND_RESISTANCE;
			case "elemental_defend_earth" -> StatEnum.EARTH_RESISTANCE;
			case "concentration" -> StatEnum.CONCENTRATION;
			case "heal_skill_boost" -> StatEnum.HEAL_SKILL_BOOST;
			case "physical_critical_reduce_rate" -> StatEnum.PHYSICAL_CRITICAL_REDUCE_RATE;
			case "physical_critical_damage_reduce" -> StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE;
			case "magical_critical_reduce_rate" -> StatEnum.MAGICAL_CRITICAL_REDUCE_RATE;
			case "magical_critical_damage_reduce" -> StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE;
			default -> null;
		};
	}

	private static int attributeInt(XMLStreamReader reader, String name) {
		String value = reader.getAttributeValue(null, name);
		return value == null ? 0 : Integer.parseInt(value);
	}
}
