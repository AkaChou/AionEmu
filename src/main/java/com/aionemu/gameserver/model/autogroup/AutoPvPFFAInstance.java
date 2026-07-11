package com.aionemu.gameserver.model.autogroup;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.PvPArenaReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.utils.PacketSendUtility;

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
		if (agt.isGloryArena()) {
			if (!decrease(player, 186000185, 4)) {
				players.remove(player.getObjectId());
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(instanceMaskId, 5));
				if (players.isEmpty()) {
					GameCoreGameplayServices.autoGroupService().unRegisterInstance(instance.getInstanceId());
				}
				return;
			}
		}
		((PvPArenaReward) instance.getInstanceHandler().getInstanceReward()).portToPosition(player);
		instance.register(player.getObjectId());
	}

	/** 离开副本 / On Leave Instance*/
	@Override
	public void onLeaveInstance(Player player) {
		super.unregister(player);
	}
}
