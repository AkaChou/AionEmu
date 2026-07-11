package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.transfers.AStation;

/**
 * 向客户端同步跨服/A-Station 服务器标识与等级限制。
 * Server packet synchronizing A-Station server id and level limits to the client.
 *
 * @author Ranastic
 */
public class SM_SERVER_IDS extends AionServerPacket {

	private AStation settings;

	/**
	 * 使用给定参数构造 SM_SERVER_IDS 包。
	 * Creates a SM_SERVER_IDS packet with the given parameters.
	 *
	 * A-Station settings
	 */
	public SM_SERVER_IDS(AStation settings) {
		this.settings = settings;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(settings.getServerId());
		writeH(0);
		writeH(settings.getIconSet());
		writeD(settings.getMaxLevel());
		writeD(settings.getMaxLevel());
		writeD(1);
		writeC(0);
	}
}
