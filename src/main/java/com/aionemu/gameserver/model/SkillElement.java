package com.aionemu.gameserver.model;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 技能元素枚举。
 * Skill Element enumeration.
 * @author xavier
 */
public enum SkillElement {

	/** 无 / None. */
	NONE(0),
	/** 火 / Fire */
	FIRE(1),
	/** 水 / Water */
	WATER(2),
	/** 风 / Wind */
	WIND(3),
	/** 地 / Earth */
	EARTH(4),
	/** 光 / Light */
	LIGHT(5),
	/** 暗 / Dark */
	DARK(6);

	private final int element;

	SkillElement(int id) {
		this.element = id;
	}

	/** 返回元素 ID / Returns the element id */
	public int getElementId() {
		return element;
	}

	/** 返回元素的抗性 / Returns the resistance for element*/
	public static StatEnum getResistanceForElement(SkillElement element) {
        return switch (element) {
            case FIRE -> StatEnum.FIRE_RESISTANCE;
            case WATER -> StatEnum.WATER_RESISTANCE;
            case WIND -> StatEnum.WIND_RESISTANCE;
            case EARTH -> StatEnum.EARTH_RESISTANCE;
            case LIGHT -> StatEnum.ELEMENTAL_RESISTANCE_LIGHT;
            case DARK -> StatEnum.ELEMENTAL_RESISTANCE_DARK;
            default -> null;
        };
    }
}
