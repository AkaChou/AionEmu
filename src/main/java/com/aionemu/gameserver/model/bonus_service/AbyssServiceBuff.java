package com.aionemu.gameserver.model.bonus_service;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.model.templates.abyss_bonus.AbyssPenaltyAttr;
import com.aionemu.gameserver.model.templates.abyss_bonus.AbyssServiceAttr;
import com.aionemu.gameserver.skillengine.change.Func;

/**
 * 欧比斯服务 Buff，用于加成服务相关逻辑。
 * Abyss Service Buff for bonus service logic.
 */

public class AbyssServiceBuff implements StatOwner {
	private List<IStatFunction> functions = new ArrayList<IStatFunction>();
	private AbyssServiceAttr abyssBonusAttr;

	public AbyssServiceBuff(int buffId) {
		abyssBonusAttr = DataManager.ABYSS_BUFF_DATA.getInstanceBonusattr(buffId);
	}

	/** 应用欧比斯效果。 / Apply abyss effect. */
	public void applyAbyssEffect(Player player, int buffId) {
		if (abyssBonusAttr == null) {
			return;
		}
		for (AbyssPenaltyAttr abyssPenaltyAttr : abyssBonusAttr.getPenaltyAttr()) {
			if (abyssPenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions.add(new StatRateFunction(abyssPenaltyAttr.getStat(), abyssPenaltyAttr.getValue(), true));
			} else {
				functions.add(new StatAddFunction(abyssPenaltyAttr.getStat(), abyssPenaltyAttr.getValue(), true));
			}
		}
		player.setAbyssBonus(true);
		player.getGameStats().addEffect(this, functions);
	}

	/** 结束效果 / End Effect */
	public void endEffect(Player player, int buffId) {
		functions.clear();
		player.setAbyssBonus(false);
		player.getGameStats().endEffect(this);
	}
}
