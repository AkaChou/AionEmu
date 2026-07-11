package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 疾病效果：标记 DISEASE 异常，阻止 HP 治疗生效。
 * Disease effect: marks DISEASE abnormal, blocking HP heal effectiveness.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DiseaseEffect")
public class DiseaseEffect extends EffectTemplate {

	/**
	 * 按疾病抗性计算是否命中。
	 * Calculates hit using disease resistance.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, StatEnum.DISEASE_RESISTANCE, null);
	}

	// skillId 18386
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
	 * 设置疾病异常。
	 * Sets the disease abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		Creature effected = effect.getEffected();
		effect.setAbnormal(AbnormalState.DISEASE.getId());
		effected.getEffectController().setAbnormal(AbnormalState.DISEASE.getId());
	}

	/**
	 * 清除疾病异常。
	 * Clears the disease abnormal.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		if (effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.DISEASE)) {
			effect.getEffected().getEffectController().unsetAbnormal(AbnormalState.DISEASE.getId());
		}
	}
}
