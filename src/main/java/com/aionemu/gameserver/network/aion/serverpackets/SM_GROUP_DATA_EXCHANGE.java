package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端转发小队自定义数据交换内容的服务端包。
 * Server packet that forwards custom group data-exchange payloads to the client.
 */
public class SM_GROUP_DATA_EXCHANGE extends AionServerPacket {
	private byte[] byteData;
	private int action;
	private int unk2;

	/**
	 * @param byteData 交换的二进制数据 / Binary payload to exchange
	 * Action type
	 * @param unk2 未知字段 / Unknown field
	 */
	public SM_GROUP_DATA_EXCHANGE(byte[] byteData, int action, int unk2) {
		this.action = action;
		this.byteData = byteData;
		this.unk2 = unk2;
	}

	/**
	 * 默认 action=1 的数据交换构造。
	 * Data-exchange constructor with default action=1.
	 *
	 * @param byteData 交换的二进制数据 / Binary payload to exchange
	 */
	public SM_GROUP_DATA_EXCHANGE(byte[] byteData) {
		this.action = 1;
		this.byteData = byteData;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action); // action

		if (action != 1) {
			writeC(unk2); // 未知 / unk
		}
		writeD(byteData.length);
		writeB(byteData);
	}
}
