package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IntervalSkillEffect")
public class IntervalSkillEffect extends AbstractOverTimeEffect {

	@XmlAttribute(name = "skill_id", required = true)
	protected int skillId;
	@XmlAttribute(name = "skill_level")
	protected int skillLevel;

	@Override
	public void onPeriodicAction(Effect effect) {
		SkillEngine.getInstance().applyEffectDirectly(
			skillId, effect.getEffector(), effect.getEffected(), 0, skillLevel);
	}
}
