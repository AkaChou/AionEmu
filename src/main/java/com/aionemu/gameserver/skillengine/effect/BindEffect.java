package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillType;

/**
 * 定身效果：施加 BIND 异常，中断正在读条的物理技能。
 * Bind effect: applies BIND abnormal and cancels casting physical skills.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BindEffect")
public class BindEffect extends EffectTemplate {

	/**
	 * 将效果加入受影响者的效果控制器。
	 * Adds the effect to the effected creature's effect controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 按定身抗性计算是否命中。
	 * Calculates hit using bind resistance.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.BIND_RESISTANCE, null);
	}

	/**
	 * 设置定身异常；若目标正在施放物理技能则取消。
	 * Sets bind abnormal; cancels a physical skill being cast.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		final Creature effected = effect.getEffected();
		effect.setAbnormal(AbnormalState.BIND.getId());
		effected.getEffectController().setAbnormal(AbnormalState.BIND.getId());
		if (effected.getCastingSkill() != null
				&& effected.getCastingSkill().getSkillTemplate().getType() == SkillType.PHYSICAL) {
			effected.getController().cancelCurrentSkill();
		}
	}

	/**
	 * 清除定身异常。
	 * Clears the bind abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.BIND.getId());
	}
}
