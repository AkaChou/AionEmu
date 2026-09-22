package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端发送军团徽章元信息（含名称与数据大小）的服务端包。
 * Server packet that sends legion emblem metadata (including name and data size) to the client.
 * @author Simple modified cura
 */
@AllArgsConstructor
public class SM_LEGION_SEND_EMBLEM extends AionServerPacket {

	/** 军团信息 / Legion information */
	private final int legionId;
	private final int emblemId;
	private final int color_r;
	private final int color_g;
	private final int color_b;
	private final String legionName;
	private final LegionEmblemType emblemType;
	private final int emblemDataSize;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(legionId);
		writeC(emblemId);
		writeC(emblemType.getValue());
		writeD(emblemDataSize);
		writeC(emblemType.equals(LegionEmblemType.DEFAULT) ? 0x00 : 0xFF);
		writeC(color_r);
		writeC(color_g);
		writeC(color_b);
		writeS(legionName);
		writeC(0x01);
	}
}
