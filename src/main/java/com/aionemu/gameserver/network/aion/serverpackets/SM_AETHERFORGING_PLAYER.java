package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步玩家以太锻造状态的服务端包。
 * Server packet synchronizing a player's Aetherforging status to the client.
 *
 * @author Ranastic
 */
public class SM_AETHERFORGING_PLAYER extends AionServerPacket {
	private int playerObjId;
	private int type;

	/**
	 * 使用玩家对象与状态类型构造同步包。
	 * Creates a sync packet from a player and status type.
	 *
	 * target player
	 * @param type 锻造状态类型 / forging status type
	 */
	public SM_AETHERFORGING_PLAYER(Player player, int type) {
		this.playerObjId = player.getObjectId();
		this.type = type;
	}

	@Override
	protected void writeImpl(AionConnection client) {
		writeD(playerObjId);
		writeC(type);
	}
}
