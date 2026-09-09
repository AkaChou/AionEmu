package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 对 CM_RESTORE_CHARACTER 的应答，返回角色恢复结果。
 * Response to CM_RESTORE_CHARACTER returning character-restore success or failure.
 *
 * @author -Nemesiss-
 */
@AllArgsConstructor
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
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(success ? 0x00 : 0x10);// 未知 / unk
		writeD(chaOid);
	}
}
