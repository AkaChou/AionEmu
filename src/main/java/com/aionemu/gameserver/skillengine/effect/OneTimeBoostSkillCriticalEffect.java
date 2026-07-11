package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.AttackerCriticalStatus;
import com.aionemu.gameserver.controllers.observer.AttackerCriticalStatusObserver;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 一次性技能暴击加成：在有限次数内强制/提升攻击暴击判定。
 * One-time skill-critical boost: forces/raises critical status for limited attacks.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OneTimeBoostSkillCriticalEffect")
public class OneTimeBoostSkillCriticalEffect extends EffectTemplate {

	@XmlAttribute
	private int count;
	@XmlAttribute
	private boolean percent;

	/**
	 * 将一次性暴击加成加入控制器。
	 * Attaches the one-time critical boost to the controller.
	 */
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 注册有限次数暴击状态检查。
	 * Registers limited-count critical status checks.
	 */
	@Override
	public void startEffect(final Effect effect) {
		super.startEffect(effect);

		AttackerCriticalStatusObserver observer = new AttackerCriticalStatusObserver(AttackStatus.CRITICAL, count,
				value, percent) {

			public AttackerCriticalStatus checkAttackerCriticalStatus(AttackStatus stat, boolean isSkill) {
				if ((stat == status) && (isSkill)) {
					if (getCount() <= 1)
						effect.endEffect();
					else {
						decreaseCount();
					}
					acStatus.setResult(true);
				} else {
					acStatus.setResult(false);
				}
				return acStatus;
			}
		};
		effect.getEffected().getObserveController().addAttackCalcObserver(observer);
		effect.setAttackStatusObserver(observer, position);
	}

	/**
	 * 移除一次性暴击加成。
	 * Removes the one-time critical boost.
	 */
	public void endEffect(Effect effect) {
		super.endEffect(effect);

		AttackCalcObserver observer = effect.getAttackStatusObserver(position);
		effect.getEffected().getObserveController().removeAttackCalcObserver(observer);
	}

	/**
	 * 是否为百分比暴击加成。
	 * Whether the critical boost is percent-based.
	 */
	public boolean isPercent() {
		return percent;
	}
}
