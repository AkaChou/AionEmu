package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.calc.Stat2;

/**
 * Summoned 对象游戏属性，用于属性相关逻辑。
 * Summoned Object Game Stats for stats logic.
 *
 * @author ATracer
 */
public class SummonedObjectGameStats extends NpcGameStats {

	public SummonedObjectGameStats(Npc owner) {
		super(owner);
	}

	/** 获取属性。 / Returns the stat. */
	@Override
	public Stat2 getStat(StatEnum statEnum, int base) {
		Stat2 stat = super.getStat(statEnum, base);
		if (owner.getMaster() == null)
			return stat;
		switch (statEnum) {
		case BOOST_MAGICAL_SKILL:
		case MAGICAL_ATTACK:
		case MAGICAL_ACCURACY:
		case MAGICAL_RESIST:
			stat.setBonusRate(0.2f);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		case PHYSICAL_ACCURACY:
			stat.setBonusRate(0.2f);
			owner.getMaster().getGameStats().getItemStatBoost(StatEnum.MAIN_HAND_ACCURACY, stat);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		case PHYSICAL_ATTACK:
			stat.setBonusRate(0.2f);
			owner.getMaster().getGameStats().getItemStatBoost(StatEnum.MAIN_HAND_POWER, stat);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);

		}
		return stat;
	}

	/** 返回 m attack / Returns the m attack */
	@Override
	public Stat2 getMAttack() {
		int power = owner.getObjectTemplate().getStatsTemplate().getPower();
		return getStat(StatEnum.MAGICAL_ATTACK, power);
	}
}
