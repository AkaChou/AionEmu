package com.aionemu.gameserver.services.conquerors;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.model.templates.serial_killer.RankPenaltyAttr;
import com.aionemu.gameserver.model.templates.serial_killer.RankRestriction;
import com.aionemu.gameserver.skillengine.change.Func;

/**
 * 征服者等级属性惩罚与增益应用器。
 * Applies conqueror rank penalty and buff attributes.
 */
public class ConquerorBuffs implements StatOwner {
	private List<IStatFunction> functions = new ArrayList<IStatFunction>();
	private RankRestriction rankRestriction;

	/**
	 * 按击杀等级对玩家施加属性效果。
	 * Applies rank-based attribute effects to the player.
	 *
	 * target player
	 * @param rank 击杀等级 / killer rank
	 */
	public void applyEffect(Player player, int rank) {
		if (rank == 0) {
			return;
		}
		rankRestriction = DataManager.SERIAL_KILLER_DATA.getRankRestriction(rank);
		if (hasDebuff()) {
			endEffect(player);
		}
		for (RankPenaltyAttr rankPenaltyAttr : rankRestriction.getPenaltyAttr()) {
			if (rankPenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions.add(new StatRateFunction(rankPenaltyAttr.getStat(), rankPenaltyAttr.getValue(), true));
			} else {
				functions.add(new StatAddFunction(rankPenaltyAttr.getStat(), rankPenaltyAttr.getValue(), true));
			}
		}
		player.getGameStats().addEffect(this, functions);
	}

	/**
	 * 是否已有生效中的属性效果。
	 * Whether an active attribute effect is currently applied.
	 *
	 * @return 有效果时为 true / true if effect functions exist
	 */
	public boolean hasDebuff() {
		return !functions.isEmpty();
	}

	/**
	 * 结束并移除玩家身上的征服者属性效果。
	 * Ends and removes conqueror attribute effects from the player.
	 *
	 * target player
	 */
	public void endEffect(Player player) {
		functions.clear();
		player.getGameStats().endEffect(this);
	}
}
