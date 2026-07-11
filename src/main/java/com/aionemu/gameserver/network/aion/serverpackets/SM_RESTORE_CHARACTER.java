package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 对 CM_RESTORE_CHARACTER 的应答，返回角色恢复结果。
 * Response to CM_RESTORE_CHARACTER returning character-restore success or failure.
 *
 * @author -Nemesiss-
 */
public class SM_RESTORE_CHARACTER extends AionServerPacket {

	/**
	 * 角色对象 ID / Character object id
	 */
	private final int chaOid;
	/**
	 * 若为真则玩家曾 restored。 / True if player was restored
	 */
	private final boolean success;

	/**
	 * 使用给定参数构造 SM_RESTORE_CHARACTER 包。
	 * Constructs new <tt>SM_RESTORE_CHARACTER </tt> packet
	 *
	 * character object id
	 * success flag
	 */
	public SM_RESTORE_CHARACTER(int chaOid, boolean success) {
		this.chaOid = chaOid;
		this.success = success;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(success ? 0x00 : 0x10);// 未知 / unk
		writeD(chaOid);
	}
}
