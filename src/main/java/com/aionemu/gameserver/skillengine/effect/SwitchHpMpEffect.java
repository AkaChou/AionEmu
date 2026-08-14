package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * HP/MP 互换效果：按配置比例交换目标当前生命与魔法值。
 * Switch HP/MP effect: exchanges configured percentages of current HP and MP.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SwitchHpMpEffect")
public class SwitchHpMpEffect extends EffectTemplate {

	@XmlAttribute(name = "mpvalue")
	private int mpValue;

	@XmlAttribute(name = "mpdelta")
	private int mpDelta;

	/**
	 * 真实公式：reserved1/2 计算 HP 交换比例，reserved3/4 计算 MP 交换比例。
	 * Retail formula: reserved1/2 calculate the HP rate; reserved3/4 calculate the MP rate.
	 */
	@Override
	public void applyEffect(Effect effect) {
		CreatureLifeStats<? extends Creature> lifeStats = effect.getEffected().getLifeStats();
		int currentHp = lifeStats.getCurrentHp();
		int currentMp = lifeStats.getCurrentMp();
		int hpRate = value + delta * effect.getSkillLevel();
		int mpRate = mpValue + mpDelta * effect.getSkillLevel();
		if (hpRate == 0 && mpRate == 0) {
			hpRate = mpRate = 100; // Backward compatibility for old templates without retail fields.
		}
		int hpAmount = (int) (hpRate / 100f * currentHp);
		int mpAmount = (int) (mpRate / 100f * currentMp);
		int newHp = Math.max(1, Math.min(lifeStats.getMaxHp(), currentHp - hpAmount + mpAmount));
		int newMp = Math.max(1, Math.min(lifeStats.getMaxMp(), currentMp + hpAmount - mpAmount));

		lifeStats.increaseHp(TYPE.NATURAL_HP, newHp - currentHp);
		lifeStats.increaseMp(TYPE.NATURAL_MP, newMp - currentMp);
	}
}
