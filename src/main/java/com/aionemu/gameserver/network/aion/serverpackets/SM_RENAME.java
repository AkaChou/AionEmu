package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端广播角色改名结果。
 * Server packet broadcasting a character rename result to the client.
 *
 * @author Rhys2002
 */
public class SM_RENAME extends AionServerPacket {

	private int playerObjectId;
	private String oldName;
	private String newName;

	/**
	 * 使用给定参数构造 SM_RENAME 包。
	 * Creates a SM_RENAME packet with the given parameters.
	 *
	 * player object id
	 * old name
	 * new name
	 */
	public SM_RENAME(int playerObjectId, String oldName, String newName) {
		this.playerObjectId = playerObjectId;
		this.oldName = oldName;
		this.newName = newName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(0); // 未知 / unk
		writeD(0); // unk - 0 or 3
		writeD(playerObjectId);
		writeS(oldName);
		writeS(newName);
	}
}
