package com.aionemu.gameserver.model.bonus_service;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.model.templates.bonus_service.PlayersBonusPenaltyAttr;
import com.aionemu.gameserver.model.templates.bonus_service.PlayersBonusServiceAttr;
import com.aionemu.gameserver.skillengine.change.Func;

/**
 * Players 加成，用于加成服务相关逻辑。
 * Players Bonus for bonus service logic.
 *
 * @author Ranastic (Encom)
 */

public class PlayersBonus implements StatOwner {
	private List<IStatFunction> functions = new ArrayList<IStatFunction>();
	private PlayersBonusServiceAttr playersServiceBonusattr;

	public PlayersBonus(int buffId) {
		playersServiceBonusattr = DataManager.PLAYERS_BONUS_DATA.getInstanceBonusattr(buffId);
	}

	/** 应用效果。 / Apply effect. */
	public void applyEffect(Player player, int buffId) {
		if (playersServiceBonusattr == null) {
			return;
		}
		for (PlayersBonusPenaltyAttr playersBonusPenaltyAttr : playersServiceBonusattr.getPenaltyAttr()) {
			if (playersBonusPenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions.add(new StatRateFunction(playersBonusPenaltyAttr.getStat(),
						playersBonusPenaltyAttr.getValue(), true));
			} else {
				functions.add(new StatAddFunction(playersBonusPenaltyAttr.getStat(), playersBonusPenaltyAttr.getValue(),
						true));
			}
		}
		player.getGameStats().addEffect(this, functions);
	}

	/** 结束效果 / End Effect */
	public void endEffect(Player player, int buffId) {
		functions.clear();
		player.setPlayersBonusId(1);
		player.getGameStats().endEffect(this);
	}
}
