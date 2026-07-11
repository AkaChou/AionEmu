package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端更新当前/最大 HP 值。
 * Server packet updating current and max HP values on the client.
 *
 * @author Luno
 */
public class SM_STATUPDATE_HP extends AionServerPacket {

	private int currentHp;
	private int maxHp;

	/**
	 * 使用给定参数构造 SM_STATUPDATE_HP 包。
	 * Creates a SM_STATUPDATE_HP packet with the given parameters.
	 *
	 * current hp
	 * max hp
	 */
	public SM_STATUPDATE_HP(int currentHp, int maxHp) {
		this.currentHp = currentHp;
		this.maxHp = maxHp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(currentHp);
		writeD(maxHp);
	}
}
