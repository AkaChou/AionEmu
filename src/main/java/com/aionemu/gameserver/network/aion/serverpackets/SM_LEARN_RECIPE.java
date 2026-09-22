package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端学会新配方的服务端包。
 * Server packet notifying the client that a new recipe has been learned.
 * @author lord_rex
 */
@AllArgsConstructor
public class SM_LEARN_RECIPE extends AionServerPacket {

	private final int recipeId;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(recipeId);
		writeC(0);
	}
}
