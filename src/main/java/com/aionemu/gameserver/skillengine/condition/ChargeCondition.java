package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 充能条件：在属性计算路径上校验物品充能等级是否达到要求。
 * Charge condition: on the stat path, validates the item charge level meets the requirement.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChargeCondition")
public class ChargeCondition extends Condition {

	@XmlAttribute(name = "level")
	private int level;

	/**
	 * 校验属性函数所属物品的充能等级。
	 * Validates the charge level of the item owning the stat function.
	 *
	 * @param env 属性对象 / stat object
	 * @param statFunction 属性函数 / stat function
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Stat2 env, IStatFunction statFunction) {
		StatOwner owner = statFunction.getOwner();
		if (owner instanceof Item) {
			Item item = (Item) owner;
			return item.getChargeLevel() >= level;
		}
		return false;
	}

	/**
	 * 技能施放路径不支持充能条件，恒为失败。
	 * Charge condition is not supported on the skill cast path; always fails.
	 *
	 * @param env 技能环境 / skill environment
	 * @return 恒为 false / always false
	 */
	@Override
	public boolean validate(Skill env) {
		return false;
	}
}
