package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局 Rule 模板（静态数据/XML）。
 * XML template.
 *
 * @author Wnkrz
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

	@XmlAttribute(name = "rule_name", required = true)
	protected String ruleName;

	@XmlAttribute(name = "min_count")
	protected Long minCount;

	@XmlAttribute(name = "max_count")
	protected Long maxCount;

	@XmlAttribute(name = "base_chance", required = true)
	protected float chance;

	@XmlAttribute(name = "min_diff")
	protected int minDiff;

	@XmlAttribute(name = "max_diff")
	protected int maxDiff;

	@XmlAttribute(name = "restriction_race")
	protected RestrictionRace restrictionRace;

	@XmlAttribute(name = "no_reduction")
	protected boolean noReduction;

	/** 返回 global rule items / Returns the global rule items */
	public GlobalDropItems getGlobalRuleItems() {
		return gdItems;
	}

	/** 设置物品。 / Sets the items. */
	public void setItems(GlobalDropItems value) {
		this.gdItems = value;
	}

	/** 返回 global rule worlds / Returns the global rule worlds */
	public GlobalDropWorlds getGlobalRuleWorlds() {
		return gdWorlds;
	}

	/** 设置 worlds / Sets the worlds */
	public void setWorlds(GlobalDropWorlds value) {
		this.gdWorlds = value;
	}

	/** 返回 global rule races / Returns the global rule races */
	public GlobalDropRaces getGlobalRuleRaces() {
		return gdRaces;
	}

	/** 设置 npc races / Sets the npc races */
	public void setNpcRaces(GlobalDropRaces value) {
		this.gdRaces = value;
	}

	/** 返回 global rule ratings / Returns the global rule ratings */
	public GlobalDropRatings getGlobalRuleRatings() {
		return gdRatings;
	}

	/** 设置 npc ratings / Sets the npc ratings */
	public void setNpcRatings(GlobalDropRatings value) {
		this.gdRatings = value;
	}

	/** 返回 global rule maps / Returns the global rule maps */
	public GlobalDropMaps getGlobalRuleMaps() {
		return gdMaps;
	}

	/** 设置 maps / Sets the maps */
	public void setMaps(GlobalDropMaps value) {
		this.gdMaps = value;
	}

	/** 返回 global rule tribes / Returns the global rule tribes */
	public GlobalDropTribes getGlobalRuleTribes() {
		return gdTribes;
	}

	/** 设置 npc tribes / Sets the npc tribes */
	public void setNpcTribes(GlobalDropTribes value) {
		this.gdTribes = value;
	}

	/** 返回 global rule npcs / Returns the global rule npcs */
	public GlobalDropNpcs getGlobalRuleNpcs() {
		return gdNpcs;
	}

	/** 设置 npcs / Sets the npcs */
	public void setNpcs(GlobalDropNpcs value) {
		this.gdNpcs = value;
	}

	/** 返回 global rule zones / Returns the global rule zones */
	public GlobalDropZones getGlobalRuleZones() {
		return gdZones;
	}

	/** 设置 zones / Sets the zones */
	public void setZones(GlobalDropZones value) {
		this.gdZones = value;
	}

	/** 返回 rule name / Returns the rule name */
	public String getRuleName() {
		return ruleName;
	}

	/** 设置 rule name / Sets the rule name */
	public void setRuleName(String value) {
		this.ruleName = value;
	}

	/** 返回最小数量 / Returns the min count*/
	public long getMinCount() {
		if (minCount == null) {
			return 1L;
		} else {
			return minCount;
		}
	}

	/** 设置最小数量 / Sets the min count*/
	public void setMinCount(Long value) {
		this.minCount = value;
	}

	/** 返回最大数量 / Returns the max count*/
	public long getMaxCount() {
		if (maxCount == null) {
			return 1L;
		} else {
			return maxCount;
		}
	}

	/** 设置最大数量 / Sets the max count*/
	public void setMaxCount(Long value) {
		this.maxCount = value;
	}

	/** 返回概率 / Returns the chance*/
	public float getChance() {
		return chance;
	}

	/** 设置概率 / Sets the chance*/
	public void setChance(float value) {
		this.chance = value;
	}

	/** 返回 min diff / Returns the min diff */
	public int getMinDiff() {
		return minDiff;
	}

	/** 设置 min diff / Sets the min diff */
	public void setMinDiff(int value) {
		this.minDiff = value;
	}

	/** 返回 max diff / Returns the max diff */
	public int getMaxDiff() {
		return maxDiff;
	}

	/** 设置 max diff / Sets the max diff */
	public void setMaxDiff(int value) {
		this.maxDiff = value;
	}

	/** 返回 restriction race / Returns the restriction race */
	public RestrictionRace getRestrictionRace() {
		return restrictionRace;
	}

	/** 设置 restriction race / Sets the restriction race */
	public void setRestrictionRace(RestrictionRace value) {
		this.restrictionRace = value;
	}

	/** 返回 no reduction / Returns the no reduction */
	public boolean getNoReduction() {
		return noReduction;
	}

	/** 设置 no reduction / Sets the no reduction */
	public void setNoReduction(boolean value) {
		this.noReduction = value;
	}

	@XmlType(name = "race_restriction")
	@XmlEnum
	public enum RestrictionRace {
		ASMODIANS, ELYOS
	}
}
