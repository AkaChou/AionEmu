package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步军团成员自我介绍更新的服务端包。
 * Server packet that synchronizes a legion member self-introduction update to the client.
 *
 * @author Simple
 */
public class SM_LEGION_UPDATE_SELF_INTRO extends AionServerPacket {

	private String selfintro;
	private int playerObjId;

	/**
	 * 使用玩家对象 ID 与自我介绍构造更新包。
	 * Creates an update packet from a player object id and self-introduction text.
	 *
	 * player object id
	 * self-introduction
	 */
	public SM_LEGION_UPDATE_SELF_INTRO(int playerObjId, String selfintro) {
		this.selfintro = selfintro;
		this.playerObjId = playerObjId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeS(selfintro);
	}
}
