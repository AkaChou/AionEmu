package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.model.gameobjects.Homing;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.calc.Stat2;

/**
 * 追踪弹（魔法飞弹）的游戏属性：继承主人的魔法攻击加成。
 * Game stats for homing projectiles: inherits the master's magical attack bonus.
 */

public class HomingGameStats extends SummonedObjectGameStats {
	public HomingGameStats(Npc owner) {
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
		case MAGICAL_ATTACK:
			stat.setBonusRate(0.2f);
			return owner.getMaster().getGameStats().getItemStatBoost(statEnum, stat);
		}
		return stat;
	}

	/** 返回魔法攻击（按等级定值）。 / Returns the magical attack (fixed value per level). */
	@Override
	public Stat2 getMAttack() {
		Homing homing = (Homing) owner;
		int power = homing.getObjectTemplate().getStatsTemplate().getPower();
		int level = homing.getObjectTemplate().getLevel();
		switch (level) {
		case 10:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 260;
			}
			break;
		case 15:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 360;
			}
			break;
		case 20:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 460;
			}
			break;
		case 25:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 560;
			}
			break;
		case 30:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 660;
			}
			break;
		case 35:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 760;
			}
			break;
		case 40:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 860;
			}
			break;
		case 45:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 960;
			}
			break;
		case 49:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 1060;
			}
			break;
		case 53:
		case 57:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 1160;
			}
			break;
		case 61:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 1260;
			}
			break;
		case 65:
			if (homing.getName().equals("Energy of Cyclone") || homing.getName().equals("Energy of Wind")) {
				power = 1360;
			}
			break;
		}
		return getStat(StatEnum.MAGICAL_ATTACK, power);
	}
}
