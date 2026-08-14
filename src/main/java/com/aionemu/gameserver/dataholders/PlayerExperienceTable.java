package com.aionemu.gameserver.dataholders;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 玩家经验表，保存各等级所需经验。
 * Player experience table holding experience required for each obtainable level.
 *
 * @author Luno
 */
@XmlRootElement(name = "player_experience_table")
@XmlAccessorType(XmlAccessType.NONE)
public class PlayerExperienceTable {

	/** 经验表 / experience table */
	@XmlElement(name = "exp")
	private long[] experience;

	/**
	 * 返回玩家在指定等级起始时拥有的经验值（例如 1 级为 0）。
	 * Returns the experience a player has at the start of the given level (e.g. 0 at level 1).
	 *
	 * @param level 等级 / level
	 * @return 玩家在指定等级开始时的经验值（如 1 级为 0） / start experience at the given level (e.g. 0 at level 1)
	 * @throws IllegalArgumentException 等级超过最大上限 / if level exceeds max
	 */
	public long getStartExpForLevel(int level) {
		if (level > experience.length) {
			throw new IllegalArgumentException("The given level is higher than possible max");
		}
		return level == 0 ? 0 : experience[level - 1];
	}

	/**
	 * 根据经验值计算对应等级。
	 * Calculates the level for the given experience value.
	 *
	 * @param expValue 经验值 / experience value
	 * @return 对应等级 / corresponding level
	 */
	public int getLevelForExp(long expValue) {
		int level = 0;
		for (int i = experience.length; i > 0; i--) {
			if (expValue >= experience[(i - 1)]) {
				level = i;
				break;
			}
		}
		if (getMaxLevel() <= level) {
			return getMaxLevel() - 1;
		}
		return level;
	}

	/**
	 * 返回玩家可达到的最大等级。
	 * Returns the maximum level a player can obtain.
	 *
	 * @return 玩家可达到的最高等级 / Returns the maximum level a player can obtain.
	 */
	public int getMaxLevel() {
		return experience == null ? 0 : experience.length;
	}
}
