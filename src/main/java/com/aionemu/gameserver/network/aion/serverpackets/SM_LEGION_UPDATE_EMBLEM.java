package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步军团徽章更新结果的服务端包。
 * Server packet that synchronizes an updated legion emblem to the client.
 *
 * @author Simple modified cura
 */
public class SM_LEGION_UPDATE_EMBLEM extends AionServerPacket {

	/** Legion emblem information  / Legion emblem information * */
	private int legionId;
	private int emblemId;
	private int color_r;
	private int color_g;
	private int color_b;
	private LegionEmblemType emblemType;

	/**
	 * 使用更新后的徽章信息构造同步包。
	 * Creates a sync packet from the updated emblem information.
	 *
	 * legion id
	 * emblem id
	 * @param color_r 红色分量 / red channel
	 * @param color_g 绿色分量 / green channel
	 * @param color_b 蓝色分量 / blue channel
	 * emblem type
	 */
	public SM_LEGION_UPDATE_EMBLEM(int legionId, int emblemId, int color_r, int color_g, int color_b,
			LegionEmblemType emblemType) {
		this.legionId = legionId;
		this.emblemId = emblemId;
		this.color_r = color_r;
		this.color_g = color_g;
		this.color_b = color_b;
		this.emblemType = emblemType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(legionId);
		writeC(emblemId);
		writeC(emblemType.getValue());
		writeC(0xFF); // Fixed
		writeC(color_r);
		writeC(color_g);
		writeC(color_b);
	}
}
