package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 条件治疗效果：受击时若生命/魔法低于阈值则立即治疗并结束效果。
 * Case-heal effect: on being attacked, heals once if HP/MP is below a threshold, then ends.
 */
public class CaseHealEffect extends AbstractHealEffect {

	@XmlAttribute(name = "cond_value")
	protected int condValue;

	@XmlAttribute
	protected HealType type;

	/**
	 * 返回当前 HP/MP。
	 * Returns current HP/MP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * @return 当前属性值 / current stat value
	 */
	protected int getCurrentStatValue(Effect effect) {
		if (type == HealType.HP) {
			return effect.getEffected().getLifeStats().getCurrentHp();
		}
		if (type == HealType.MP) {
			return effect.getEffected().getLifeStats().getCurrentMp();
		}
		return 0;
	}

	/**
	 * 返回 HP/MP 上限。
	 * Returns max HP/MP.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * @return 属性上限 / max stat value
	 */
	protected int getMaxStatValue(Effect effect) {
		if (type == HealType.HP) {
			return effect.getEffected().getGameStats().getMaxHp().getCurrent();
		}
		if (type == HealType.MP) {
			return effect.getEffected().getGameStats().getMaxMp().getCurrent();
		}
		return 0;
	}

	/**
	 * 将效果加入受影响者的效果控制器。
	 * Adds the effect to the effected creature's effect controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 移除受击观察者。
	 * Removes the attacked observer.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void endEffect(Effect effect) {
		ActionObserver observer = effect.getActionObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeObserver(observer);
		}
	}

	/**
	 * 注册受击观察者：低于阈值时治疗并结束效果。
	 * Registers an attacked observer: heals when below threshold and ends the effect.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void startEffect(final Effect effect) {
		ActionObserver observer = new ActionObserver(ObserverType.ATTACKED) {

			public void attacked(Creature creature) {
				int valueWithDelta = value + delta * effect.getSkillLevel();
				int currentValue = getCurrentStatValue(effect);
				int maxValue = getMaxStatValue(effect);
				int possibleHealValue = 0;

				if (currentValue <= maxValue * condValue / 100) {
					if (percent)
						possibleHealValue = maxValue * valueWithDelta / 100;
					else {
						possibleHealValue = valueWithDelta;
					}
					int finalHeal = possibleHealValue;
					if (type == HealType.HP && effect.getSkillTemplate().isHealBoostApplied()) {
						finalHeal = effect.getEffector().getGameStats()
								.getStat(StatEnum.HEAL_SKILL_BOOST, possibleHealValue).getCurrent();
					}

					finalHeal = maxValue - currentValue < finalHeal ? maxValue - currentValue : finalHeal;

					if ((type == HealType.HP)
							&& (effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.DISEASE))) {
						finalHeal = 0;
					}

					if (type == HealType.HP) {
						effect.getEffected().getLifeStats().increaseHp(TYPE.HP, finalHeal, effect.getSkillId(),
								LOG.REGULAR);
					} else if (type == HealType.MP) {
						effect.getEffected().getLifeStats().increaseMp(TYPE.MP, finalHeal, effect.getSkillId(),
								LOG.REGULAR);
					}
					AbstractHealEffect.notifyHealedByUser(effect, type, getCurrentStatValue(effect) - currentValue);
					effect.endEffect();
				}
			}
		};
		effect.getEffected().getObserveController().addObserver(observer);
		effect.setActionObserver(observer, position);
	}
}
