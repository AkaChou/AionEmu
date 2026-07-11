package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步 A-Station（跨服中转站）服务器切换信息的服务端包。
 * Server packet synchronizing A-Station (cross-server hub) server-switch information.
 *
 * @author Ranastic
 */
public class SM_A_STATION extends AionServerPacket {
	private boolean isFirst = false;
	private int currentServer = 0;
	private int newServerId = 0;

	/**
	 * @param currentServer 当前服务器 ID / current server id
	 * @param newServerId 目标服务器 ID / destination server id
	 * @param first 是否为首次进入 A-Station / whether this is the first entry into A-Station
	 */
	public SM_A_STATION(int currentServer, int newServerId, boolean first) {
		this.currentServer = currentServer;
		this.newServerId = newServerId;
		this.isFirst = first;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		writeD(newServerId);
		writeD(currentServer);
		writeD(player.getObjectId());
		if (isFirst) {
			writeD(NetworkConfig.GAMESERVER_ID);
		} else {
			writeD(newServerId);
		}
		writeD(0);
		writeD(0);
	}
}
