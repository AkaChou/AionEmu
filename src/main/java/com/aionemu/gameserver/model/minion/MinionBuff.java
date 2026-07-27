package com.aionemu.gameserver.model.minion;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.minion.MinionTemplate;

/**
 * 守护灵 Buff 模型。
 * Minion Buff model.
 */

public class MinionBuff implements StatOwner {

	/** 应用。 / Apply. */
	public void apply(Player player, int minionId) {
		if (minionId == 0) {
			return;
		}
		MinionTemplate minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
		List<IStatFunction> functions = new ArrayList<IStatFunction>();
		for (StatFunction statFunction : minionTemplate.getModifiers()) {
			// if
			// (player.getPlayerClass().getClassType(player).equals(statFunction.getClassType()))
			// {
			functions.add(new StatAddFunction(statFunction.getName(), statFunction.getValue(), true));
			// }
		}
		player.getGameStats().addEffect(this, functions);
	}

	/** 结束 / end. */
	public void end(Player player) {
		player.getGameStats().endEffect(this);
	}
}
