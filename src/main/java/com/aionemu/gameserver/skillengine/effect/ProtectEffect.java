package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.AttackCalcObserver;
import com.aionemu.gameserver.controllers.observer.AttackShieldObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 保护效果：作为护盾类效果，代受/减免被保护者所受伤害。
 * Protect effect: shield-like effect that absorbs/reduces damage for the protected.
 *
 * @author Sippolo
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProtectEffect")
public class ProtectEffect extends ShieldEffect {

	/**
	 * 启动保护：设置护盾观察与召唤/死亡联动。
	 * Starts protect: shield observers and summon/death linkage.
	 */
	@Override
	public void startEffect(final Effect effect) {
		int skillLevel = effect.getSkillLevel();
		int protectedDamage = getHitValue(skillLevel);
		int protectorDamage = value + delta * skillLevel;
		AttackShieldObserver asObserver = new AttackShieldObserver(protectedDamage, protectorDamage, radius, percent,
				effect, this.hitType, this.getType(), getHitTypeProbability(skillLevel));

		effect.getEffected().getObserveController().addAttackCalcObserver(asObserver);
		effect.setAttackShieldObserver(asObserver, position);
		if ((effect.getEffector() instanceof Summon)) {
			ActionObserver summonRelease = new ActionObserver(ObserverType.SUMMONRELEASE) {

				public void summonrelease() {
					effect.endEffect();
				}
			};
			effect.getEffector().getObserveController().attach(summonRelease);
			effect.setActionObserver(summonRelease, position);
		} else {
			ActionObserver death = new ActionObserver(ObserverType.DEATH) {

				public void died(Creature creature) {
					effect.endEffect();
				}
			};
			effect.getEffector().getObserveController().attach(death);
			effect.setActionObserver(death, position);
		}
	}

	/**
	 * 结束保护并移除观察者。
	 * Ends protect and removes observers.
	 */
	@Override
	public void endEffect(Effect effect) {
		AttackCalcObserver acObserver = effect.getAttackShieldObserver(position);
		if (acObserver != null) {
			effect.getEffected().getObserveController().removeAttackCalcObserver(acObserver);
		}
		ActionObserver aObserver = effect.getActionObserver(position);
		if (aObserver != null) {
			effect.getEffector().getObserveController().removeObserver(aObserver);
		}
	}

	/**
	 * 护盾类型：1 反射，2 普通护盾，8 保护。
	 * shieldType 1:reflector 2: normal shield 8: protect
	 *
	 * @return
	 */
	/**
	 * 返回保护效果类型标识。
	 * Returns the protect effect type id.
	 */
	@Override
	public int getType() {
		return 8;
	}
}
