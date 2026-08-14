package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 飞行中条件：校验相关单位当前处于飞行状态。
 * On-fly condition: validates the related unit is currently flying.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OnFlyCondition")
public class OnFlyCondition extends Condition {

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		return env.getEffector().isFlying();
	}

	/**
	 * 校验属性计算环境是否满足本条件。
	 * Validates whether the stat calculation environment satisfies this condition.
	 *
	 * @param stat 属性对象 / stat object
	 * @param statFunction 属性函数 / stat function
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Stat2 stat, IStatFunction statFunction) {
		return stat.getOwner().isFlying();
	}

	/**
	 * 校验效果环境是否满足本条件。
	 * Validates whether the effect environment satisfies this condition.
	 *
	 * @param effect 效果环境 / effect environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Effect effect) {
		return effect.getEffected().isFlying();
	}
}
