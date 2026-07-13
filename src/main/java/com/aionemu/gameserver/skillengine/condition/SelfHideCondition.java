package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Skill;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SelfHideCondition")
public class SelfHideCondition extends Condition {

	@Override
	public boolean validate(Skill skill) {
		return skill.getEffector() != null
			&& skill.getEffector().getEffectController().isAbnormalSet(AbnormalState.HIDE);
	}
}
