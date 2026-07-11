package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端学会新配方的服务端包。
 * Server packet notifying the client that a new recipe has been learned.
 *
 * @author lord_rex
 */
public class SM_LEARN_RECIPE extends AionServerPacket {

	private int recipeId;

	/**
	 * 构造学会配方通知包。
	 * Creates a packet announcing a newly learned recipe.
	 *
	 * recipe id
	 */
	public SM_LEARN_RECIPE(int recipeId) {
		this.recipeId = recipeId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(recipeId);
		writeC(0);
	}
}
