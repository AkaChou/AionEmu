package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步军团徽章更新结果的服务端包。
 * Server packet that synchronizes an updated legion emblem to the client.
 *
 * @author Simple modified cura
 */
@AllArgsConstructor
public class SM_LEGION_UPDATE_EMBLEM extends AionServerPacket {

	/** Legion emblem information  / Legion emblem information * */
	private final int legionId;
	private final int emblemId;
	private final int color_r;
	private final int color_g;
	private final int color_b;
	private final LegionEmblemType emblemType;

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
