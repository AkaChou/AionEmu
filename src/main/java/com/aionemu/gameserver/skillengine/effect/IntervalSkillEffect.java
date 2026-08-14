package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 周期性技能效果：每个周期直接对目标施放指定技能（跳过常规计算流程）。
 * Interval skill effect: directly applies the specified skill to the target on each period (bypassing the regular calculation flow).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IntervalSkillEffect")
public class IntervalSkillEffect extends AbstractOverTimeEffect {

	@XmlAttribute(name = "skill_id", required = true)
	protected int skillId;
	@XmlAttribute(name = "skill_level")
	protected int skillLevel;

	@Override
	public void onPeriodicAction(Effect effect) {
		GameEngineServices.skillEngine().applyEffectDirectly(
			skillId, effect.getEffector(), effect.getEffected(), 0, skillLevel);
	}
}
