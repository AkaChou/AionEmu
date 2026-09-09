package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端更新当前/最大 HP 值。
 * Server packet updating current and max HP values on the client.
 *
 * @author Luno
 */
@AllArgsConstructor
public class SM_STATUPDATE_HP extends AionServerPacket {

	private final int currentHp;
	private final int maxHp;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(currentHp);
		writeD(maxHp);
	}
}
