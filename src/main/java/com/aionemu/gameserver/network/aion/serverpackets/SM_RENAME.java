package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端广播角色改名结果。
 * Server packet broadcasting a character rename result to the client.
 *
 * @author Rhys2002
 */
@AllArgsConstructor
public class SM_RENAME extends AionServerPacket {

	private final int playerObjectId;
	private final String oldName;
	private final String newName;

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
