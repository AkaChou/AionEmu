package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 角色删除响应包：回应 CM_DELETE_CHARACTER，返回角色对象 ID 与删除时间戳。
 * Response to CM_DELETE_CHARACTER: character object id and deletion timestamp.
 *
 * @author -Nemesiss-
 */
public class SM_DELETE_CHARACTER extends AionServerPacket {

	private int playerObjId;
	private int deletionTime;

	/**
	 * @param playerObjId  角色对象 ID；0 表示失败 / character object id; 0 means failure
	 * @param deletionTime 删除时间戳 / deletion timestamp
	 */
	public SM_DELETE_CHARACTER(int playerObjId, int deletionTime) {
		this.playerObjId = playerObjId;
		this.deletionTime = deletionTime;
	}

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
