package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端通知神石销毁结果的服务端包。
 * Server packet that notifies the client of a godstone destruction result.
 *
 * @author wanke
 */
public class SM_GODSTONE_DESTROY extends AionServerPacket {

	private int objectId;
	private int godstoneId;
	private int time;

	/**
	 * @param objectId 物品对象 ID / Item object ID
	 * @param godstoneId 神石 ID / Godstone ID
	 * @param time 相关时间参数 / Related time parameter
	 */
	public SM_GODSTONE_DESTROY(int objectId, int godstoneId, int time) {
		this.objectId = objectId;
		this.godstoneId = godstoneId;
		this.time = time;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(objectId);
		writeD(godstoneId);
		writeD(time);
	}
}
