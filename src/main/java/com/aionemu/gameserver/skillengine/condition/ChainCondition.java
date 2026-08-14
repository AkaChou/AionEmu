package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 连锁技能条件：校验前序/自身连锁类别、次数与时间窗是否允许施放。
 * Chain skill condition: validates precursor/self chain category, count and time window allow casting.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChainCondition")
public class ChainCondition extends Condition {

	@XmlAttribute(name = "selfcount")
	private int selfCount;
	@XmlAttribute(name = "precount")
	private int preCount;
	@XmlAttribute(name = "category")
	private String category;
	@XmlAttribute(name = "precategory")
	private String precategory;
	@XmlAttribute(name = "time")
	private int time;

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		if ((env.getEffector() instanceof Player) && (precategory != null || selfCount > 0)) {
			Player pl = (Player) env.getEffector();

			if (selfCount > 0) {
				boolean canUse = false;

				if (precategory != null && pl.getChainSkills().chainSkillEnabled(precategory, time)) {
					canUse = true;
				}

				if (pl.getChainSkills().chainSkillEnabled(category, time)) {
					canUse = true;
				} else if (precategory == null) {
					canUse = true;
				}

				if (!canUse) {
					return false;
				}
				if (selfCount <= pl.getChainSkills().getChainCount(pl, env.getSkillTemplate(), category)) {
					return false;
				} else {
					env.setIsMultiCast(true);
				}
			} else if (preCount > 0) {
				if (!pl.getChainSkills().chainSkillEnabled(precategory, time)
						|| preCount != pl.getChainSkills().getChainCount(pl, env.getSkillTemplate(), precategory)) {
					return false;
				}
			} else if (!pl.getChainSkills().chainSkillEnabled(precategory, time)) {
				return false;
			}
		}
		env.setChainCategory(category);
		return true;
	}

	/**
	 * 获取自身连锁允许次数上限。
	 * Gets the self-chain allowed count limit.
	 *
	 * @return 自身连锁次数 / self chain count
	 */
	public int getSelfCount() {
		return selfCount;
	}

	/**
	 * 获取本技能所属连锁类别。
	 * Gets the chain category this skill belongs to.
	 *
	 * @return 连锁类别 / chain category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * 获取连锁时间窗（毫秒）。
	 * Gets the chain time window in milliseconds.
	 *
	 * time window
	 */
	public int getTime() {
		return time;
	}
}
