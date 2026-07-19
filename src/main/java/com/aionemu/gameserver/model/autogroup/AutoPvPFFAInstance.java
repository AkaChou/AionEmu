package com.aionemu.gameserver.model.autogroup;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.PvPArenaReward;

/**
 * 自动 PvPFFA 副本，用于 autogroup 相关逻辑。
 * Auto Pv PFFA Instance for autogroup logic.
 */

public class AutoPvPFFAInstance extends AutoInstance {
	/** 添加玩家。 / Adds player. */
	@Override
	public AGQuestion addPlayer(Player player, SearchInstance searchInstance) {
		super.writeLock();
		try {
			if (!satisfyTime(searchInstance) || (players.size() >= agt.getPlayerSize())) {
				return AGQuestion.FAILED;
			}
			byte side = allocateSide(1, player.getRace());
			if (side < 0) {
				return AGQuestion.FAILED;
			}
			AGPlayer matchPlayer = new AGPlayer(player);
			matchPlayer.setMatchSide(side);
			players.put(player.getObjectId(), matchPlayer);
			return instance != null ? AGQuestion.ADDED
					: (agt.isCompositionReady(players.values()) ? AGQuestion.READY : AGQuestion.ADDED);
		} finally {
			super.writeUnlock();
		}
	}

	/** 按下回车时 / on Press Enter. */
	@Override
	public void onPressEnter(Player player) {
		enter(player, () -> {
			((PvPArenaReward) instance.getInstanceHandler().getInstanceReward()).portToPosition(player);
			return true;
		});
	}

	/** 离开副本 / On Leave Instance*/
	@Override
	public void onLeaveInstance(Player player) {
		super.unregister(player);
	}
}
