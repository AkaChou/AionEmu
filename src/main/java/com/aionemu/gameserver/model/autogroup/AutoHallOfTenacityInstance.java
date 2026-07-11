package com.aionemu.gameserver.model.autogroup;

import com.aionemu.gameserver.lifecycle.GameBattlefieldServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.HallOfTenacityReward;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;

/**
 * 自动 HallOfTenacity 副本，用于 autogroup 相关逻辑。
 * Auto Hall Of Tenacity Instance for autogroup logic.
 *
 * @author Ranastic
 */
public class AutoHallOfTenacityInstance extends AutoInstance {
	/** 添加玩家。 / Adds player. */
	@Override
	public AGQuestion addPlayer(Player player, SearchInstance searchInstance) {
		super.writeLock();
		try {
			if (!satisfyTime(searchInstance) || (players.size() >= agt.getPlayerSize())) {
				return AGQuestion.FAILED;
			}
			players.put(player.getObjectId(), new AGPlayer(player));
			return instance != null ? AGQuestion.ADDED
					: (players.size() == agt.getPlayerSize() ? AGQuestion.READY : AGQuestion.ADDED);
		} finally {
			super.writeUnlock();
		}
	}

	/** 按下回车时 / on Press Enter. */
	@Override
	public void onPressEnter(Player player) {
		super.onPressEnter(player);
		GameBattlefieldServices.hallOfTenacityService().addCoolDown(player);
		((HallOfTenacityReward) instance.getInstanceHandler().getInstanceReward()).portToHall(player);
		instance.register(player.getObjectId());
	}

	/** 离开副本 / On Leave Instance*/
	@Override
	public void onLeaveInstance(Player player) {
		super.unregister(player);
		PlayerGroupService.removePlayer(player);
	}
}
