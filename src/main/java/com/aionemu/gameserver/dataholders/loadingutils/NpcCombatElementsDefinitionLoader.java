package com.aionemu.gameserver.dataholders.loadingutils;

import java.io.File;
import java.io.FileInputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.npc.NpcCombatElements;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;

/**
 * 从真端投影加载 NPC 战斗元素覆盖层。
 * Loads the retail-projected NPC combat-element overlay.
 * <p>真端 {@code npcs.xml} 的 360 余种元素中，本地模板此前只承载了属性数值与异常免疫位图；
 * 命中、减伤、元素防御、异常抗性/穿透、PvE 比率、召唤继承等元素由本覆盖层补齐。
 * Of the 360+ retail {@code npcs.xml} elements, the local templates previously carried only
 * stat values and the abnormal-immunity bitmask; hit, mitigation, elemental defence, abnormal
 * resistance/penetration, PvE ratios and summon inheritance are supplied here.
 */
final class NpcCombatElementsDefinitionLoader {

	/** 眩晕类异常对应的穿透属性。 / Penetration stats for stun-like abnormals. */
	private static final List<StatEnum> STUNLIKE_PENETRATIONS = List.of(
		StatEnum.CHARM_RESISTANCE_PENETRATION, StatEnum.OPENAREIAL_RESISTANCE_PENETRATION,
		StatEnum.SPIN_RESISTANCE_PENETRATION, StatEnum.STAGGER_RESISTANCE_PENETRATION,
		StatEnum.STUMBLE_RESISTANCE_PENETRATION, StatEnum.STUN_RESISTANCE_PENETRATION);

	/** 全部具体异常穿透属性（不含组属性）。 / All concrete abnormal penetrations. */
	private static final List<StatEnum> ALL_PENETRATIONS = List.of(
		StatEnum.BLEED_RESISTANCE_PENETRATION, StatEnum.BIND_RESISTANCE_PENETRATION,
		StatEnum.BLIND_RESISTANCE_PENETRATION, StatEnum.CHARM_RESISTANCE_PENETRATION,
		StatEnum.CONFUSE_RESISTANCE_PENETRATION, StatEnum.CURSE_RESISTANCE_PENETRATION,
		StatEnum.DEFORM_RESISTANCE_PENETRATION, StatEnum.DISEASE_RESISTANCE_PENETRATION,
		StatEnum.FEAR_RESISTANCE_PENETRATION, StatEnum.NOFLY_RESISTANCE_PENETRATION,
		StatEnum.OPENAREIAL_RESISTANCE_PENETRATION, StatEnum.PARALYZE_RESISTANCE_PENETRATION,
		StatEnum.PERIFICATION_RESISTANCE_PENETRATION, StatEnum.POISON_RESISTANCE_PENETRATION,
		StatEnum.PULLED_RESISTANCE_PENETRATION, StatEnum.ROOT_RESISTANCE_PENETRATION,
		StatEnum.SILENCE_RESISTANCE_PENETRATION, StatEnum.SIMPLE_ROOT_RESISTANCE_PENETRATION,
		StatEnum.SLEEP_RESISTANCE_PENETRATION, StatEnum.SLOW_RESISTANCE_PENETRATION,
		StatEnum.SNARE_RESISTANCE_PENETRATION, StatEnum.SPIN_RESISTANCE_PENETRATION,
		StatEnum.STAGGER_RESISTANCE_PENETRATION, StatEnum.STUMBLE_RESISTANCE_PENETRATION,
		StatEnum.STUN_RESISTANCE_PENETRATION);

	/**
	 * 读取紧凑定义并挂载到对应 NPC 模板。
	 * Reads the compact definition and attaches it to the matching NPC templates.
	 *
	 * @param file    定义文件 / definition file
	 * @param npcData NPC 数据容器 / NPC data holder
	 * @return 应用了战斗元素的模板数 / number of templates that received combat elements
	 */
	static int apply(File file, NpcData npcData) {
		int applied = 0;
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
		try (FileInputStream stream = new FileInputStream(file)) {
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			NpcCombatElements current = null;
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					switch (reader.getLocalName()) {
						case "npc" -> {
							NpcTemplate template = npcData.getNpcTemplate(attributeInt(reader, "id", 0));
							if (template == null) {
								current = null;
								continue;
							}
							current = build(reader);
							template.setCombatElements(current);
							applied++;
						}
						case "abnormal_resist" -> {
							if (current != null) {
								addResistance(current, attribute(reader, "name", ""), attributeInt(reader, "value", 0));
							}
						}
						case "abnormal_penetration" -> {
							if (current != null) {
								addPenetration(current, attribute(reader, "name", ""), attributeInt(reader, "value", 0));
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
	 * 读取 {@code <npc>} 上的标量战斗元素。
	 * Reads the scalar combat elements on {@code <npc>}.
	 *
	 * @param reader XML 读取器 / XML reader
	 * @return 战斗元素 / combat elements
	 */
	private static NpcCombatElements build(XMLStreamReader reader) {
		NpcCombatElements elements = new NpcCombatElements();
		elements.setHitCount(attributeInt(reader, "hit_count", 1));
		elements.putStatBase(StatEnum.HIT_COUNT, elements.getHitCount());
		put(reader, elements, "dp", StatEnum.MAXDP);
		put(reader, elements, "concentration", StatEnum.CONCENTRATION);
		put(reader, elements, "magical_hit_accuracy", StatEnum.MAGICAL_ACCURACY);
		put(reader, elements, "physical_critical_reduce_rate", StatEnum.PHYSICAL_CRITICAL_REDUCE_RATE);
		put(reader, elements, "magical_critical_reduce_rate", StatEnum.MAGICAL_CRITICAL_REDUCE_RATE);
		put(reader, elements, "physical_critical_damage_reduce", StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE);
		put(reader, elements, "magical_critical_damage_reduce", StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE);
		put(reader, elements, "proc_reduce_rate", StatEnum.PROC_REDUCE_RATE);
		put(reader, elements, "npc_pve_attack_ratio_physical", StatEnum.PVE_ATTACK_RATIO_PHYSICAL);
		put(reader, elements, "npc_pve_attack_ratio_magical", StatEnum.PVE_ATTACK_RATIO_MAGICAL);
		put(reader, elements, "npc_pve_defend_ratio_physical", StatEnum.PVE_DEFEND_RATIO_PHYSICAL);
		put(reader, elements, "npc_pve_defend_ratio_magical", StatEnum.PVE_DEFEND_RATIO_MAGICAL);
		put(reader, elements, "elemental_defend_water", StatEnum.WATER_RESISTANCE);
		put(reader, elements, "elemental_defend_air", StatEnum.WIND_RESISTANCE);
		put(reader, elements, "elemental_defend_earth", StatEnum.EARTH_RESISTANCE);
		put(reader, elements, "elemental_defend_fire", StatEnum.FIRE_RESISTANCE);
		put(reader, elements, "elemental_defend_light", StatEnum.ELEMENTAL_RESISTANCE_LIGHT);
		put(reader, elements, "elemental_defend_dark", StatEnum.ELEMENTAL_RESISTANCE_DARK);
		elements.setDpReduce(attributeInt(reader, "dp_reduce", 0));
		elements.setCancelLevel(attributeInt(reader, "cancel_level", 0));
		elements.setPhysicalDamageTrim(attributeInt(reader, "physical_damage_trim", 0) > 0);
		elements.setMagicalDamageTrim(attributeInt(reader, "magical_damage_trim", 0) > 0);
		elements.setNoReduceSpellImmune(attributeInt(reader, "noreducespell_immune", 0) > 0);
		elements.setLimitAttribute(attribute(reader, "limit_attribute", null));
		addElementalImmunities(elements, attribute(reader, "elemental_immunity", ""));
		return elements;
	}

	/**
	 * 读取一个数值属性并写入属性表。
	 * Reads one numeric attribute into the stat table.
	 */
	private static void put(XMLStreamReader reader, NpcCombatElements elements, String attribute, StatEnum stat) {
		int value = attributeInt(reader, attribute, 0);
		if (value != 0) {
			elements.putStatBase(stat, value);
		}
	}

	/**
	 * 添加一条异常抗性（名称可为逗号分隔的多个标记）。
	 * Adds one abnormal resistance (the name may be a comma-separated token list).
	 */
	private static void addResistance(NpcCombatElements elements, String name, int value) {
		if (value == 0) {
			return;
		}
		EnumSet<StatEnum> stats = EnumSet.noneOf(StatEnum.class);
		for (String token : name.split(",")) {
			StatEnum stat = resistanceStat(token.trim());
			if (stat != null) {
				stats.add(stat);
			}
		}
		stats.forEach(stat -> elements.putStatBase(stat, value));
	}

	/**
	 * 添加一条异常穿透（名称可为逗号分隔的多个标记）。
	 * Adds one abnormal penetration (the name may be a comma-separated token list).
	 */
	private static void addPenetration(NpcCombatElements elements, String name, int value) {
		if (value == 0) {
			return;
		}
		EnumSet<StatEnum> stats = EnumSet.noneOf(StatEnum.class);
		for (String token : name.split(",")) {
			String normalized = token.trim().toLowerCase(Locale.ROOT);
			if (normalized.isEmpty()) {
				continue;
			}
			if ("stat_arall".equals(normalized)) {
				stats.addAll(ALL_PENETRATIONS);
			} else if ("stat_arstunlike".equals(normalized)) {
				stats.addAll(STUNLIKE_PENETRATIONS);
			} else {
				StatEnum stat = penetrationStat(normalized);
				if (stat != null) {
					stats.add(stat);
				}
			}
		}
		stats.forEach(stat -> elements.putStatBase(stat, value));
	}

	/**
	 * 将真端异常抗性标记映射为本地属性。
	 * Maps a retail abnormal-resistance token to a local stat.
	 */
	static StatEnum resistanceStat(String token) {
		return switch (token.toLowerCase(Locale.ROOT)) {
			case "stat_arall" -> StatEnum.ABNORMAL_RESISTANCE_ALL;
			case "stat_arstunlike" -> StatEnum.STUNLIKE_RESISTANCE;
			case "sleep" -> StatEnum.SLEEP_RESISTANCE;
			case "bleed" -> StatEnum.BLEED_RESISTANCE;
			case "poison" -> StatEnum.POISON_RESISTANCE;
			case "paralyze" -> StatEnum.PARALYZE_RESISTANCE;
			case "fear" -> StatEnum.FEAR_RESISTANCE;
			case "openaerial" -> StatEnum.OPENAREIAL_RESISTANCE;
			case "deform" -> StatEnum.DEFORM_RESISTANCE;
			case "stun" -> StatEnum.STUN_RESISTANCE;
			case "stumble" -> StatEnum.STUMBLE_RESISTANCE;
			case "spin" -> StatEnum.SPIN_RESISTANCE;
			case "stagger" -> StatEnum.STAGGER_RESISTANCE;
			case "pulled" -> StatEnum.PULLED_RESISTANCE;
			case "silence" -> StatEnum.SILENCE_RESISTANCE;
			case "blind" -> StatEnum.BLIND_RESISTANCE;
			case "charm" -> StatEnum.CHARM_RESISTANCE;
			case "confuse" -> StatEnum.CONFUSE_RESISTANCE;
			case "disease" -> StatEnum.DISEASE_RESISTANCE;
			case "root" -> StatEnum.ROOT_RESISTANCE;
			case "snare" -> StatEnum.SNARE_RESISTANCE;
			case "slow" -> StatEnum.SLOW_RESISTANCE;
			case "bind" -> StatEnum.BIND_RESISTANCE;
			case "nofly" -> StatEnum.NOFLY_RESISTANCE;
			case "curse" -> StatEnum.CURSE_RESISTANCE;
			case "perification", "petrification" -> StatEnum.PERIFICATION_RESISTANCE;
			default -> null;
		};
	}

	/**
	 * 将真端异常穿透标记映射为本地属性。
	 * Maps a retail abnormal-penetration token to a local stat.
	 */
	static StatEnum penetrationStat(String token) {
		return switch (token) {
			case "sleep" -> StatEnum.SLEEP_RESISTANCE_PENETRATION;
			case "bleed" -> StatEnum.BLEED_RESISTANCE_PENETRATION;
			case "poison" -> StatEnum.POISON_RESISTANCE_PENETRATION;
			case "paralyze" -> StatEnum.PARALYZE_RESISTANCE_PENETRATION;
			case "fear" -> StatEnum.FEAR_RESISTANCE_PENETRATION;
			case "openaerial" -> StatEnum.OPENAREIAL_RESISTANCE_PENETRATION;
			case "deform" -> StatEnum.DEFORM_RESISTANCE_PENETRATION;
			case "stun" -> StatEnum.STUN_RESISTANCE_PENETRATION;
			case "stumble" -> StatEnum.STUMBLE_RESISTANCE_PENETRATION;
			case "spin" -> StatEnum.SPIN_RESISTANCE_PENETRATION;
			case "stagger" -> StatEnum.STAGGER_RESISTANCE_PENETRATION;
			case "pulled" -> StatEnum.PULLED_RESISTANCE_PENETRATION;
			case "silence" -> StatEnum.SILENCE_RESISTANCE_PENETRATION;
			case "blind" -> StatEnum.BLIND_RESISTANCE_PENETRATION;
			case "charm" -> StatEnum.CHARM_RESISTANCE_PENETRATION;
			case "confuse" -> StatEnum.CONFUSE_RESISTANCE_PENETRATION;
			case "disease" -> StatEnum.DISEASE_RESISTANCE_PENETRATION;
			case "root" -> StatEnum.ROOT_RESISTANCE_PENETRATION;
			case "snare" -> StatEnum.SNARE_RESISTANCE_PENETRATION;
			case "slow" -> StatEnum.SLOW_RESISTANCE_PENETRATION;
			case "bind" -> StatEnum.BIND_RESISTANCE_PENETRATION;
			case "nofly" -> StatEnum.NOFLY_RESISTANCE_PENETRATION;
			case "curse" -> StatEnum.CURSE_RESISTANCE_PENETRATION;
			case "perification", "petrification" -> StatEnum.PERIFICATION_RESISTANCE_PENETRATION;
			default -> null;
		};
	}

	/**
	 * 解析元素免疫列表（erfire、erwater、erair、erearth、erlight、erdark）。
	 * Parses the elemental-immunity list.
	 */
	private static void addElementalImmunities(NpcCombatElements elements, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		for (String token : value.split(",")) {
			SkillElement element = switch (token.trim().toLowerCase(Locale.ROOT)) {
				case "erfire" -> SkillElement.FIRE;
				case "erwater" -> SkillElement.WATER;
				case "erair" -> SkillElement.WIND;
				case "erearth" -> SkillElement.EARTH;
				case "erlight" -> SkillElement.LIGHT;
				case "erdark" -> SkillElement.DARK;
				default -> null;
			};
			if (element != null) {
				elements.addElementalImmunity(element);
			}
		}
	}

	private static int attributeInt(XMLStreamReader reader, String name, int defaultValue) {
		String value = reader.getAttributeValue(null, name);
		if (value == null || value.isEmpty()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static String attribute(XMLStreamReader reader, String name, String defaultValue) {
		String value = reader.getAttributeValue(null, name);
		return value == null || value.isEmpty() ? defaultValue : value;
	}
}
