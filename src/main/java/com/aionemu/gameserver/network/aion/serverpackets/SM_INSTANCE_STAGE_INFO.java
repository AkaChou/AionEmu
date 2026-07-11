package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步副本阶段/事件进度信息的服务端包。
 * Server packet synchronizing instance stage/event progress info to the client.
 *
 * @author xTz
 */
public class SM_INSTANCE_STAGE_INFO extends AionServerPacket {

	private int type;
	private int event;
	private int unk;

	/**
	 * 使用阶段类型、事件码与未知字段构造阶段信息包。
	 * Creates a stage-info packet from type, event code, and an unknown field.
	 *
	 * @param type 阶段类型 / stage type
	 * event code
	 * @param unk 未知字段 / unknown field
	 */
	public SM_INSTANCE_STAGE_INFO(int type, int event, int unk) {
		this.type = type;
		this.event = event;
		this.unk = unk;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(type);
		writeD(0);
		writeH(event);
		writeH(unk);
	}
}
