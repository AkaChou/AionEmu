package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 广播玩家当前目标变更的服务端包。
 * Server packet that broadcasts a player's current target change.
 *
 * @author Sweetkr
 */
public class SM_TARGET_UPDATE extends AionServerPacket {

	private Player player;

	/**
	 * @param player 目标发生变更的玩家 / player whose target changed
	 */
	public SM_TARGET_UPDATE(Player player) {
		this.player = player;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(player.getObjectId());
		writeD(player.getTarget() == null ? 0 : player.getTarget().getObjectId());
	}
}
