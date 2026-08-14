package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatCondition;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 技能条件基类：定义技能施放、效果应用与属性计算时的条件校验契约。
 * Skill condition base class: defines the validation contract for skill cast, effect apply, and stat calculation.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Condition")
public abstract class Condition implements StatCondition {

	/**
	 * 校验技能环境是否满足模板中指定的条件。
	 * Validates whether the skill environment satisfies the condition specified in the template.
	 *
	 * @param env 技能环境 / skill environment
	 * @return 是否有效 / whether valid
	 */
	public abstract boolean validate(Skill env);

	/**
	 * 校验属性计算环境是否满足本条件；默认恒为通过。
	 * Validates whether the stat calculation environment satisfies this condition; default always passes.
	 *
	 * @param stat 属性对象 / stat object
	 * @param statFunction 属性函数 / stat function
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Stat2 stat, IStatFunction statFunction) {
		return true;
	}

	/**
	 * 校验效果环境是否满足本条件；默认恒为通过。
	 * Validates whether the effect environment satisfies this condition; default always passes.
	 *
	 * @param effect 效果环境 / effect environment
	 * @return 是否有效 / whether valid
	 */
	public boolean validate(Effect effect) {
		return true;
	}
}
