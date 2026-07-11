package com.aionemu.gameserver.skillengine.effect.modifier;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 行动修正器集合：XML 绑定多种伤害/目标修正子类型。
 * Action modifiers container: JAXB binding for various damage/target modifier subtypes.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActionModifiers")
public class ActionModifiers {

	@XmlElements({ @XmlElement(name = "frontdamage", type = FrontDamageModifier.class),
			@XmlElement(name = "backdamage", type = BackDamageModifier.class),
			@XmlElement(name = "abnormaldamage", type = AbnormalDamageModifier.class),
			@XmlElement(name = "targetrace", type = TargetRaceDamageModifier.class),
			@XmlElement(name = "targetclass", type = TargetClassDamageModifier.class) })
	protected List<ActionModifier> actionModifiers;

	/**
	 * 获取行动修正器列表（懒初始化）。
	 * Returns the action modifiers list (lazy-initialized).
	 *
	 * @return 修正器列表 / modifiers list
	 */
	public List<ActionModifier> getActionModifiers() {
		if (actionModifiers == null) {
			actionModifiers = new ArrayList<ActionModifier>();
		}
		return this.actionModifiers;
	}
}
