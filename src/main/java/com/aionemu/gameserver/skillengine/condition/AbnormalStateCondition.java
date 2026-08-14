package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 异常状态条件：校验目标是否处于指定异常状态。
 * Abnormal state condition: validates the target is under the specified abnormal state.
 *
 * @author kecimis
 */
public class AbnormalStateCondition extends Condition {

	@XmlAttribute(required = true)
	protected AbnormalState value;

	/**
	 * 校验技能首目标是否带有指定异常状态。
	 * Validates whether the skill's first target has the specified abnormal state.
	 *
	 * @param env 技能环境 / skill environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		if (env.getFirstTarget() != null) {
			return value == AbnormalState.STUNLIKE
				? env.getFirstTarget().getEffectController().isAbnormalState(value)
				: env.getFirstTarget().getEffectController().isAbnormalSet(value);
		}
		return false;
	}

	/**
	 * 校验效果作用目标是否带有指定异常状态。
	 * Validates whether the effect's target has the specified abnormal state.
	 *
	 * @param effect 效果环境 / effect environment
	 * @return 是否有效 / whether valid
	 */
	@Override
	public boolean validate(Effect effect) {
		if (effect.getEffected() != null) {
			return value == AbnormalState.STUNLIKE
				? effect.getEffected().getEffectController().isAbnormalState(value)
				: effect.getEffected().getEffectController().isAbnormalSet(value);
		}
		return false;
	}
}
