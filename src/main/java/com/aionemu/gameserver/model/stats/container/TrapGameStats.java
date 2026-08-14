package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.calc.Stat2;

/**
 * 陷阱的游戏属性：继承主人的魔法技能加成。
 * Game stats for traps: inherits the master's magical skill bonus.
 *
 * @author ATracer
 */
public class TrapGameStats extends NpcGameStats {

	public TrapGameStats(Npc owner) {
		super(owner);
	}

	/** 获取属性。 / Returns the stat. */
	@Override
	public Stat2 getStat(StatEnum statEnum, int base) {
		Stat2 stat = super.getStat(statEnum, base);
		if (owner.getMaster() == null) {
			return stat;
		}
		switch (statEnum) {
		case BOOST_MAGICAL_SKILL:
		case MAGICAL_ACCURACY:
			// 加成按主人属性加成计算（仅绿色数值） / bonus is calculated from stat bonus of master (only green value)
			stat.setBonusRate(0.7f);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		}
		return stat;
	}
}
