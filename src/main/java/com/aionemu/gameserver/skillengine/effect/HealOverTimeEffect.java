package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 持续治疗基类：按周期结算治疗量，供 HP/MP/FP 持续治疗效果复用。
 * Heal-over-time base: periodic heal settlement reused by HP/MP/FP HoT effects.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HealOverTimeEffect")
public abstract class HealOverTimeEffect extends AbstractOverTimeEffect {
	/**
	 * 计算持续治疗是否生效。
	 * Calculates whether the HoT applies.
	 */
	public void calculate(Effect effect, HealType healType) {
		if (!super.calculate(effect, null, null)) {
			return;
		}
		Creature effector = effect.getEffector();
		if (effect.getEffected() instanceof Npc) {
			value = effector.getAi2().modifyHealValue(value);
		}
		int valueWithDelta = value + delta * effect.getSkillLevel();
		int maxCurValue = getMaxStatValue(effect);
		int possibleHealValue = 0;
		if (percent) {
			possibleHealValue = maxCurValue * valueWithDelta / 100;
		} else {
			possibleHealValue = valueWithDelta;
		}
		int finalHeal = possibleHealValue;
		if (healType == HealType.HP) {
			int baseHeal = possibleHealValue;
			if (effect.getItemTemplate() == null && effect.getSkillTemplate().isHealBoostApplied()) {
				int boostHealAdd = effector.getGameStats().getStat(StatEnum.HEAL_BOOST, 0).getCurrent();
				int boostHeal = (effector.getGameStats().getStat(StatEnum.HEAL_BOOST, baseHeal).getCurrent()
						- boostHealAdd);
				if (boostHealAdd > 0) {
					boostHeal += boostHeal * boostHealAdd / 1000;
				}
				finalHeal = effect.getEffected().getGameStats().getStat(StatEnum.HEAL_SKILL_BOOST, boostHeal)
						.getCurrent();
			}
			finalHeal = effect.getEffected().getGameStats().getStat(StatEnum.HEAL_SKILL_DEBOOST, finalHeal)
					.getCurrent();
		}
		else if (effect.getItemTemplate() == null && healType == HealType.MP
			&& effect.getSkillTemplate().isMpHealBoostApplied()) {
			finalHeal = effector.getGameStats().getStat(StatEnum.MP_HEAL_SKILL_BOOST, possibleHealValue).getCurrent();
			finalHeal = AbstractHealEffect.capMpHealBoost(possibleHealValue, finalHeal);
		}
		effect.setReservedInt(position, finalHeal);
	}

	/**
	 * 周期结算治疗量并广播。
	 * Periodically settles heal amount and broadcasts.
	 */
	public void onPeriodicAction(Effect effect, HealType healType) {
		Creature effected = effect.getEffected();
		int currentValue = getCurrentStatValue(effect);
		int maxCurValue = getMaxStatValue(effect);
		int possibleHealValue = effect.getReservedInt(position);
		int healValue = maxCurValue - currentValue < possibleHealValue ? (maxCurValue - currentValue)
				: possibleHealValue;
		if (healValue <= 0) {
			return;
		}
		switch (healType) {
		case HP:
			effected.getLifeStats().increaseHp(TYPE.HP, healValue, effect.getSkillId(), LOG.HEAL);
			break;
		case MP:
			effected.getLifeStats().increaseMp(TYPE.MP, healValue, effect.getSkillId(), LOG.MPHEAL);
			break;
		case FP:
			((Player) effected).getLifeStats().increaseFp(TYPE.FP, healValue, effect.getSkillId(), LOG.FPHEAL);
			break;
		case DP:
			((Player) effected).getCommonData().addDp(healValue);
			break;
		}
		if (healType == HealType.HP || healType == HealType.MP) {
			AbstractHealEffect.notifyHealedByUser(effect, healType, getCurrentStatValue(effect) - currentValue);
		}
	}

	/**
	 * 返回当前对应属性值（HP/MP/FP 等）。
	 * Returns the current value of the related stat (HP/MP/FP, etc.).
	 */
	protected abstract int getCurrentStatValue(Effect effect);

	/**
	 * 返回对应属性上限。
	 * Returns the maximum value of the related stat.
	 */
	protected abstract int getMaxStatValue(Effect effect);
}
