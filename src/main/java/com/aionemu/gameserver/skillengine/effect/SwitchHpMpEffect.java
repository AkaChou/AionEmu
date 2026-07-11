package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * HP/MP 互换效果：将目标当前生命与魔法值对调。
 * Switch HP/MP effect: swaps the target current HP and MP values.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SwitchHpMpEffect")
public class SwitchHpMpEffect extends EffectTemplate {

	/**
	 * 以当前 HP/MP 差值互相增减，实现互换。
	 * Increases HP/MP by the delta between current values to swap them.
	 */
	@Override
	public void applyEffect(Effect effect) {
		CreatureLifeStats<? extends Creature> lifeStats = effect.getEffected().getLifeStats();
		int currentHp = lifeStats.getCurrentHp();
		int currentMp = lifeStats.getCurrentMp();

		lifeStats.increaseHp(TYPE.NATURAL_HP, currentMp - currentHp);
		lifeStats.increaseMp(TYPE.NATURAL_MP, currentHp - currentMp);
	}
}
