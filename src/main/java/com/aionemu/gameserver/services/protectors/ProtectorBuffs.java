package com.aionemu.gameserver.services.protectors;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.model.templates.serial_guard.GuardRankPenaltyAttr;
import com.aionemu.gameserver.model.templates.serial_guard.GuardRankRestriction;
import com.aionemu.gameserver.model.templates.serial_guard.GuardTypePenaltyAttr;
import com.aionemu.gameserver.model.templates.serial_guard.GuardTypeRestriction;
import com.aionemu.gameserver.skillengine.change.Func;

/**
 * 守护者等级/类型属性惩罚与增益应用器。
 * Applies protector rank/type penalty and buff attributes.
 */
public class ProtectorBuffs implements StatOwner {
	private GuardRankRestriction guardRankRestriction;
	private GuardTypeRestriction guardTypeRestriction;
	private List<IStatFunction> functions = new ArrayList<IStatFunction>();

	/**
	 * 按守护等级对玩家施加属性效果。
	 * Applies rank-based attribute effects to the player.
	 *
	 * target player
	 * @param rank 守护等级 / guard rank
	 */
	public void applyRankEffect(Player player, int rank) {
		if (rank == 0) {
			return;
		}
		guardRankRestriction = DataManager.SERIAL_GUARD_DATA.getGuardRankRestriction(rank);
		if (hasDebuff()) {
			endEffect(player);
		}
		for (GuardRankPenaltyAttr guardrankPenaltyAttr : guardRankRestriction.getGuardPenaltyAttr()) {
			if (guardrankPenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions.add(
						new StatRateFunction(guardrankPenaltyAttr.getStat(), guardrankPenaltyAttr.getValue(), true));
			} else {
				functions.add(
						new StatAddFunction(guardrankPenaltyAttr.getStat(), guardrankPenaltyAttr.getValue(), true));
			}
		}
		player.getGameStats().addEffect(this, functions);
	}

	/**
	 * 按守护类型对玩家施加属性效果。
	 * Applies type-based attribute effects to the player.
	 *
	 * target player
	 * @param type 守护类型 / guard type
	 */
	public void applyTypeEffect(Player player, int type) {
		if (type == 0) {
			return;
		}
		guardTypeRestriction = DataManager.SERIAL_GUARD_DATA.getGuardTypeRestriction(type);
		if (hasDebuff()) {
			endEffect(player);
		}
		for (GuardTypePenaltyAttr guardtypePenaltyAttr : guardTypeRestriction.getGuardPenaltyAttr()) {
			if (guardtypePenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions.add(
						new StatRateFunction(guardtypePenaltyAttr.getStat(), guardtypePenaltyAttr.getValue(), true));
			} else {
				functions.add(
						new StatAddFunction(guardtypePenaltyAttr.getStat(), guardtypePenaltyAttr.getValue(), true));
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
	 * 结束并移除玩家身上的守护者属性效果。
	 * Ends and removes protector attribute effects from the player.
	 *
	 * target player
	 */
	public void endEffect(Player player) {
		functions.clear();
		player.getGameStats().endEffect(this);
	}
}
