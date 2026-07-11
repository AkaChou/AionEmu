package com.aionemu.gameserver.skillengine.effect.modifier;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 异常状态伤害修正：目标处于指定异常状态时加成伤害。
 * Abnormal-state damage modifier: bonus damage when the target has a given abnormal state.
 *
 * @author kecimis
 */
public class AbnormalDamageModifier extends ActionModifier {

	@XmlAttribute(required = true)
	protected AbnormalState state;

	@Override
	public int analyze(Effect effect) {
		return (value + effect.getSkillLevel() * delta);
	}

	@Override
	public boolean check(Effect effect) {
		return effect.getEffected().getEffectController().isAbnormalSet(state);
	}
}
