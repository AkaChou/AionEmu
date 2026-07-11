package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 印记爆发效果：消耗目标身上的印记，按层数放大魔法伤害。
 * Signet-burst effect: consumes the target signet and scales magical damage by its level.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignetBurstEffect")
public class SignetBurstEffect extends DamageEffect {
	@XmlAttribute
	protected int signetlvl;

	@XmlAttribute
	protected String signet;

	/**
	 * 查找印记效果，按层数调整伤害与命中修正，结算后结束印记。
	 *
	 * @param effect Finds the signet, scales damage / accuracy by level, calculates magical result, then ends the signet.
	 */
	@Override
	public void calculate(Effect effect) {
		Effect signetEffect = effect.getEffected().getEffectController().getAnormalEffect(signet);
		if (!super.calculate(effect, DamageType.MAGICAL)) {
			if (signetEffect != null) {
				signetEffect.endEffect();
			}
			return;
		}
		int valueWithDelta = value + delta * effect.getSkillLevel();
		int critAddDmg = this.critAddDmg2 + this.critAddDmg1 * effect.getSkillLevel();
		if (signetEffect == null) {
			valueWithDelta *= 0.05f;
			effect.setLaunchSubEffect(false);
			AttackUtil.calculateMagicalSkillResult(effect, valueWithDelta, null, getElement(), true, true, false,
					getMode(), this.critProbMod2, critAddDmg, shared, false);
		} else {
			int level = signetEffect.getSkillLevel();
			if (level < 3) {
				effect.setSubEffectAborted(true);
			}
			effect.setSignetBurstedCount(level);
			switch (level) {
			case 1:
				valueWithDelta *= 0.2f;
				break;
			case 2:
				valueWithDelta *= 0.5f;
				break;
			case 3:
				valueWithDelta *= 1.0f;
				break;
			case 4:
				valueWithDelta *= 1.2f;
				break;
			case 5:
				valueWithDelta *= 1.5f;
				break;
			}
			int accmod = 0;
			int mAccurancy = effect.getEffector().getGameStats().getMAccuracy().getCurrent();
			switch (level) {
			case 1:
				accmod = (int) (-10.8f * mAccurancy);
				break;
			case 2:
				accmod = (int) (-10.5f * mAccurancy);
				break;
			case 3:
				accmod = 0;
				break;
			case 4:
				accmod = (int) (13.5f * mAccurancy);
				break;
			case 5:
				accmod = (int) (18.5f * mAccurancy);
				break;
			}
			effect.setAccModBoost(accmod);
			effect.setLaunchSubEffect(true);
			AttackUtil.calculateMagicalSkillResult(effect, valueWithDelta, null, getElement(), true, true, false,
					getMode(), this.critProbMod2, critAddDmg, shared, false);
			if (signetEffect != null) {
				signetEffect.endEffect();
			}
		}
	}
}
