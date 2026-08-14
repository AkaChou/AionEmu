package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端显示/刷新玩家的 VIP 图标状态。
 * Notifies the client to show/refresh a player's VIP icon state.
 */
public class SM_NOTIFY_VIP_ICON extends AionServerPacket {

	private final int objectId;

	/**
	 * 为目标玩家构造 VIP 图标通知包。
	 * Creates a VIP icon notification packet for the given player.
	 *
	 * @param player 目标玩家 / target player
	 */
	public SM_NOTIFY_VIP_ICON(Player player) {
		this(player.getObjectId());
	}

	SM_NOTIFY_VIP_ICON(int objectId) {
		this.objectId = objectId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeH(0);
	}
}
