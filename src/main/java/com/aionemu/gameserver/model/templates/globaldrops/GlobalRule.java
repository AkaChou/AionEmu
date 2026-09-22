package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 全局掉落规则模板（静态数据/XML）。
 * Global drop rule template (static data/XML).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalRule")
public class GlobalRule {
	@XmlElement(name = "gd_items", required = false)
	protected GlobalDropItems gdItems;

	@XmlElement(name = "gd_maps", required = false)
	protected GlobalDropMaps gdMaps;

	@XmlElement(name = "gd_races", required = false)
	protected GlobalDropRaces gdRaces;

	@XmlElement(name = "gd_tribes", required = false)
	protected GlobalDropTribes gdTribes;

	@XmlElement(name = "gd_ratings", required = false)
	protected GlobalDropRatings gdRatings;

	@XmlElement(name = "gd_worlds", required = false)
	protected GlobalDropWorlds gdWorlds;

	@XmlElement(name = "gd_npcs", required = false)
	protected GlobalDropNpcs gdNpcs;

	@XmlElement(name = "gd_zones", required = false)
	protected GlobalDropZones gdZones;

	/**
	 * -- GETTER --
	 * 返回规则名称。 / Returns the rule name.
	 * -- SETTER --
	 * 设置规则名称。 / Sets the rule name.

	 */
	@Setter
	@Getter
	@XmlAttribute(name = "rule_name", required = true)
	protected String ruleName;

	/**
	 * -- SETTER --
	 * 设置最小数量。 / Sets the min count.
	 */
	@Setter
	@XmlAttribute(name = "min_count")
	protected Long minCount;

	/**
	 * -- SETTER --
	 * 设置最大数量。 / Sets the max count.
	 */
	@Setter
	@XmlAttribute(name = "max_count")
	protected Long maxCount;

	/**
	 * -- GETTER --
	 * 返回概率。 / Returns the chance.
	 * -- SETTER --
	 * 设置概率。 / Sets the chance.

	 */
	@Setter
	@Getter
	@XmlAttribute(name = "base_chance", required = true)
	protected float chance;

	/**
	 * -- GETTER --
	 * 返回最小等级差。 / Returns the min diff.
	 * -- SETTER --
	 * 设置最小等级差。 / Sets the min diff.

	 */
	@Setter
	@Getter
	@XmlAttribute(name = "min_diff")
	protected int minDiff;

	/**
	 * -- GETTER --
	 * 返回最大等级差。 / Returns the max diff.
	 * -- SETTER --
	 * 设置最大等级差。 / Sets the max diff.

	 */
	@Setter
	@Getter
	@XmlAttribute(name = "max_diff")
	protected int maxDiff;

	/**
	 * -- GETTER --
	 * 返回种族限制。 / Returns the restriction race.
	 * -- SETTER --
	 * 设置种族限制。 / Sets the restriction race.

	 */
	@Setter
	@Getter
	@XmlAttribute(name = "restriction_race")
	protected RestrictionRace restrictionRace;

	/**
	 * -- SETTER --
	 * 设置不削减掉落标志。 / Sets the no reduction flag.
	 */
	@Setter
	@XmlAttribute(name = "no_reduction")
	protected boolean noReduction;

    /**
     * -- GETTER --
     * 返回数量是否按 NPC 等级缩放。 / Returns whether counts scale with the NPC level.
	 * -- SETTER --
	 * 设置数量是否按 NPC 等级缩放。 / Sets whether counts scale with the NPC level.

	 */
    @Setter
    @Getter
    @XmlAttribute(name = "count_per_npc_level")
	protected boolean countPerNpcLevel;

	/** 返回规则物品。 / Returns the rule items. */
	public GlobalDropItems getGlobalRuleItems() {
		return gdItems;
	}

	/** 设置物品。 / Sets the items. */
	public void setItems(GlobalDropItems value) {
		this.gdItems = value;
	}

	/** 返回规则世界。 / Returns the rule worlds. */
	public GlobalDropWorlds getGlobalRuleWorlds() {
		return gdWorlds;
	}

	/** 设置世界。 / Sets the worlds. */
	public void setWorlds(GlobalDropWorlds value) {
		this.gdWorlds = value;
	}

	/** 返回规则种族。 / Returns the rule races. */
	public GlobalDropRaces getGlobalRuleRaces() {
		return gdRaces;
	}

	/** 设置 NPC 种族。 / Sets the NPC races. */
	public void setNpcRaces(GlobalDropRaces value) {
		this.gdRaces = value;
	}

	/** 返回规则等级。 / Returns the rule ratings. */
	public GlobalDropRatings getGlobalRuleRatings() {
		return gdRatings;
	}

	/** 设置 NPC 等级。 / Sets the NPC ratings. */
	public void setNpcRatings(GlobalDropRatings value) {
		this.gdRatings = value;
	}

	/** 返回规则地图。 / Returns the rule maps. */
	public GlobalDropMaps getGlobalRuleMaps() {
		return gdMaps;
	}

	/** 设置地图。 / Sets the maps. */
	public void setMaps(GlobalDropMaps value) {
		this.gdMaps = value;
	}

	/** 返回规则部落。 / Returns the rule tribes. */
	public GlobalDropTribes getGlobalRuleTribes() {
		return gdTribes;
	}

	/** 设置 NPC 部落。 / Sets the NPC tribes. */
	public void setNpcTribes(GlobalDropTribes value) {
		this.gdTribes = value;
	}

	/** 返回规则 NPC。 / Returns the rule npcs. */
	public GlobalDropNpcs getGlobalRuleNpcs() {
		return gdNpcs;
	}

	/** 设置 NPC。 / Sets the npcs. */
	public void setNpcs(GlobalDropNpcs value) {
		this.gdNpcs = value;
	}

	/** 返回规则区域。 / Returns the rule zones. */
	public GlobalDropZones getGlobalRuleZones() {
		return gdZones;
	}

	/** 设置区域。 / Sets the zones. */
	public void setZones(GlobalDropZones value) {
		this.gdZones = value;
	}

	/** 返回最小数量。 / Returns the min count. */
	public long getMinCount() {
		if (minCount == null) {
			return 1L;
		} else {
			return minCount;
		}
	}

	/** 返回最大数量。 / Returns the max count. */
	public long getMaxCount() {
		if (maxCount == null) {
			return 1L;
		} else {
			return maxCount;
		}
	}

	/** 返回不削减掉落标志。 / Returns the no reduction flag. */
	public boolean getNoReduction() {
		return noReduction;
	}

	/** 返回指定 NPC 等级下的最小数量。 / Returns the minimum count for the NPC level. */
	public long getMinCountForNpcLevel(int npcLevel) {
		return scaleCount(getMinCount(), npcLevel);
	}

	/** 返回指定 NPC 等级下的最大数量。 / Returns the maximum count for the NPC level. */
	public long getMaxCountForNpcLevel(int npcLevel) {
		return scaleCount(getMaxCount(), npcLevel);
	}

	private long scaleCount(long count, int npcLevel) {
		return countPerNpcLevel ? Math.multiplyExact(count, Math.max(1, npcLevel)) : count;
	}

	@XmlType(name = "race_restriction")
	@XmlEnum
	public enum RestrictionRace {
		/** 魔族 / Asmodians. */
		ASMODIANS,
		/** 天族 / Elyos. */
		ELYOS
	}
}
