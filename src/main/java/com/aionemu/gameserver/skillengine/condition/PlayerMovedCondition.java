package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 玩家移动条件：校验施法者移动状态是否与 allow 配置一致。
 * Player moved condition: validates the effector move state matches the allow configuration.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PlayerMovedCondition")
public class PlayerMovedCondition extends Condition {

	@XmlAttribute(required = true)
	protected boolean allow;

	/**
	 * 获取是否允许移动施放。
	 * Gets whether moving is allowed for casting.
	 *
	 * @return 允许标记 / allow flag
	 */
	public boolean isAllow() {
		return allow;
	}

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param skill 技能环境 / skill environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Skill skill) {
		return allow == skill.getConditionChangeListener().isEffectorMoved();
	}
}
