package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 技能发射效果：立即以配置的 skill_id 对目标再施放一个技能。
 * Skill launcher effect: immediately applies another skill identified by skill_id.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillLauncherEffect")
public class SkillLauncherEffect extends EffectTemplate {

	@XmlAttribute(name = "skill_id")
	protected int skillId;

	/**
	 * 通过技能引擎应用配置的技能效果。
	 * Applies the configured skill effect through the skill engine.
	 */
	@Override
	public void applyEffect(Effect effect) {
		GameEngineServices.skillEngine().applyEffect(skillId, effect.getEffector(), effect.getEffected());
	}

	/**
	 * 返回要发射的技能 ID。
	 * Returns the skill id to launch.
	 */
	public int getLaunchSkillId() {
		return skillId;
	}
}
