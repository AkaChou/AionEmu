package com.aionemu.gameserver.skillengine.action;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.effect.modifier.ActionModifiers;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 技能动作基类：施法时执行的消耗/行为模板。
 * Base skill action: cost/behavior template executed when casting a skill.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Action")
public abstract class Action {

	/**
	 * 动作修正器集合。
	 * Action modifier collection.
	 */
	protected ActionModifiers modifiers;

	/**
	 * 按模板执行该动作。
	 * Performs the action defined by the template.
	 *
	 * @param skill 当前技能上下文 / current skill context
	 */
	public abstract void act(Skill skill);
}
