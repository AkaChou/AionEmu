package com.aionemu.gameserver.services.territory;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 军团领地增益，为进入领地的玩家提供 PvP 防御加成。
 * Legion territory buff applying PvP defense bonus to players inside territory.
 */
public class TerritoryBuff implements StatOwner {
	private List<IStatFunction> functions = new ArrayList<IStatFunction>();

	/**
	 * 对玩家施加领地增益效果。
	 * Applies the territory buff effect to the player.
	 *
	 * @param player 目标玩家 / target player
	 */
	public void applyEffect(Player player) {
		int addvalue = 60;
		if (hasBuff()) {
			endEffect(player);
		}
		functions.add(new StatAddFunction(StatEnum.PVP_DEFEND_RATIO, addvalue, true));
		player.getGameStats().addEffect(this, functions);
	}

	/**
	 * 是否已存在生效中的增益。
	 * Whether an active buff is currently applied.
	 *
	 * @return 有增益时为 true / true if buff functions exist
	 */
	public boolean hasBuff() {
		return !functions.isEmpty();
	}

	/**
	 * 结束并移除玩家身上的领地增益。
	 * Ends and removes the territory buff from the player.
	 *
	 * @param player 目标玩家 / target player
	 */
	public void endEffect(Player player) {
		functions.clear();
		player.getGameStats().endEffect(this);
	}
}
