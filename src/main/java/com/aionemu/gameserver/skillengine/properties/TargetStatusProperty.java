package com.aionemu.gameserver.skillengine.properties;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 目标状态属性：要求首要目标处于指定异常状态之一。
 * Target status property: requires the first target to have one of the listed abnormal states.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TargetStatusProperty")
public class TargetStatusProperty {

	/**
	 * 校验首要目标是否拥有配置的异常状态之一（仅支持单一目标）。
	 * Validates that the single first target has one of the configured abnormal states.
	 *
	 * @param skill 技能上下文 / skill context
	 * @param properties 目标筛选属性 / target filter properties
	 * @return 目标数不为 1 时 false；否则是否匹配任一状态 / false if not exactly one target; else whether any status matches
	 */
	public static final boolean set(final Skill skill, Properties properties) {
		if (skill.getEffectedList().size() != 1) {
			return false;
		}

		List<String> targetStatus = properties.getTargetStatus();

		Creature effected = skill.getFirstTarget();
		boolean result = false;

		for (String status : targetStatus) {
			if (effected.getEffectController().isAbnormalSet(AbnormalState.valueOf(status))) {
				result = true;
			}
		}
		return result;
	}
}
