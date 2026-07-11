package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步军团成员昵称更新的服务端包。
 * Server packet that synchronizes a legion member nickname update to the client.
 *
 * @author Simple
 */
public class SM_LEGION_UPDATE_NICKNAME extends AionServerPacket {

	private int playerObjId;
	private String newNickname;

	/**
	 * 使用玩家对象 ID 与新昵称构造更新包。
	 * Creates an update packet from a player object id and new nickname.
	 *
	 * player object id
	 * new nickname
	 */
	public SM_LEGION_UPDATE_NICKNAME(int playerObjId, String newNickname) {
		this.playerObjId = playerObjId;
		this.newNickname = newNickname;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeS(newNickname);
	}
}
