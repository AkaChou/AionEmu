package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Skill;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillGroupCondition")
public class SkillGroupCondition extends Condition {

	@XmlAttribute(required = true)
	private String value;

	@Override
	public boolean validate(Skill skill) {
		return skill.getEffector() != null
			&& skill.getEffector().getEffectController().getAnormalEffect(value) != null;
	}

	public String getValue() {
		return value;
	}
}
