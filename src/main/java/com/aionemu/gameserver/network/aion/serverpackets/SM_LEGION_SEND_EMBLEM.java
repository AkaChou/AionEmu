package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送军团徽章元信息（含名称与数据大小）的服务端包。
 * Server packet that sends legion emblem metadata (including name and data size) to the client.
 *
 * @author Simple modified cura
 */
public class SM_LEGION_SEND_EMBLEM extends AionServerPacket {

	/** 军团信息 / Legion information */
	private int legionId;
	private int emblemId;
	private int color_r;
	private int color_g;
	private int color_b;
	private String legionName;
	private LegionEmblemType emblemType;
	private int emblemDataSize;

	/**
	 * 使用徽章元信息构造发送包。
	 * Creates a send packet from emblem metadata.
	 *
	 * legion id
	 * emblem id
	 * @param color_r 红色分量 / red channel
	 * @param color_g 绿色分量 / green channel
	 * @param color_b 蓝色分量 / blue channel
	 * legion name
	 * emblem type
	 * @param emblemDataSize 徽章数据大小 / emblem data size
	 */
	public SM_LEGION_SEND_EMBLEM(int legionId, int emblemId, int color_r, int color_g, int color_b, String legionName,
			LegionEmblemType emblemType, int emblemDataSize) {
		this.legionId = legionId;
		this.emblemId = emblemId;
		this.color_r = color_r;
		this.color_g = color_g;
		this.color_b = color_b;
		this.legionName = legionName;
		this.emblemType = emblemType;
		this.emblemDataSize = emblemDataSize;
	}

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
