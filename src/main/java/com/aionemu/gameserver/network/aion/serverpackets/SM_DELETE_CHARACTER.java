package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 角色删除响应包：回应 CM_DELETE_CHARACTER，返回角色对象 ID 与删除时间戳。
 * Response to CM_DELETE_CHARACTER: character object id and deletion timestamp.
 *
 * @author -Nemesiss-
 */
@AllArgsConstructor
public class SM_DELETE_CHARACTER extends AionServerPacket {

	private final int playerObjId;
	private final int deletionTime;

	@Override
	protected void writeImpl(AionConnection con) {
		if (playerObjId != 0) {
			writeD(0x00);// 未知 / unk
			writeD(playerObjId);
			writeD(deletionTime);
		} else {
			writeD(0x10);// 未知 / unk
			writeD(0x00);
			writeD(0x00);
		}
	}
}
