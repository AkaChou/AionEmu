package com.aionemu.gameserver.model.bonus_service;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.model.templates.bonus_service.F2pBonusAttr;
import com.aionemu.gameserver.model.templates.bonus_service.F2pPenalityAttr;
import com.aionemu.gameserver.skillengine.change.Func;

/**
 * F2p 加成，用于加成服务相关逻辑。
 * F 2 p Bonus for bonus service logic.
 */

public class F2pBonus implements StatOwner {
	private List<IStatFunction> functions = new ArrayList<IStatFunction>();
	private F2pBonusAttr f2pBonusattr;

	public F2pBonus(int buffId) {
		f2pBonusattr = DataManager.F2P_BONUS_DATA.getInstanceBonusattr(buffId);
	}

	/** 应用效果。 / Apply effect. */
	public void applyEffect(Player player, int buffId) {
		if (f2pBonusattr == null) {
			return;
		}
		for (F2pPenalityAttr f2pBonusPenaltyAttr : f2pBonusattr.getPenaltyAttr()) {
			if (f2pBonusPenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions
						.add(new StatRateFunction(f2pBonusPenaltyAttr.getStat(), f2pBonusPenaltyAttr.getValue(), true));
			} else {
				functions.add(new StatAddFunction(f2pBonusPenaltyAttr.getStat(), f2pBonusPenaltyAttr.getValue(), true));
			}
		}
		player.getGameStats().addEffect(this, functions);
	}

	/** 结束效果 / End Effect */
	public void endEffect(Player player, int buffId) {
		functions.clear();
		player.getGameStats().endEffect(this);
	}
}
