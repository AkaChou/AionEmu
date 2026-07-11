package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.templates.item.ArmorType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 防具条件：校验施法者是否装备了指定类型的防具。
 * Armor condition: validates the effector has the specified armor type equipped.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArmorCondition")
public class ArmorCondition extends Condition {

	@XmlAttribute(name = "armor")
	private ArmorType armorType;

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		return isValidArmor(env.getEffector());
	}

	/**
	 * 校验属性计算环境是否满足本条件。
	 * Validates whether the stat calculation environment satisfies this condition.
	 *
	 * @param stat 属性对象 / stat object
	 * stat function
	 * whether valid
	 */
	@Override
	public boolean validate(Stat2 stat, IStatFunction statFunction) {
		return isValidArmor(stat.getOwner());
	}

	/**
	 * 校验效果环境是否满足本条件。
	 * Validates whether the effect environment satisfies this condition.
	 *
	 * effect environment
	 * whether valid
	 */
	@Override
	public boolean validate(Effect effect) {
		return isValidArmor(effect.getEffector());
	}

	/**
	 * 判断生物是否装备了指定类型防具（仅玩家有效）。
	 * Checks whether the creature has the required armor type equipped (players only).
	 *
	 * creature
	 * whether valid
	 */
	private boolean isValidArmor(Creature creature) {
		if (creature instanceof Player) {
			Player player = (Player) creature;
			return player.getEquipment().isArmorTypeEquipped(armorType);
		}
		return false;
	}
}
