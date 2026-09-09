package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 扩音器（喇叭）消息服务端包。
 * Server packet that delivers a megaphone/shout-item message to the client.
 *
 * @author (Encom)
 */
@AllArgsConstructor
public class SM_MEGAPHONE_MESSAGE extends AionServerPacket {
	private final Player player;
	private final String message;
	private final int itemId;
	private final boolean isAll;

	@Override
	protected void writeImpl(AionConnection client) {
		writeS(player.getName());
		writeS(message);
		writeD(itemId);
		writeC(this.isAll ? this.player.getRace().getRaceId() : 255);
	}
}
