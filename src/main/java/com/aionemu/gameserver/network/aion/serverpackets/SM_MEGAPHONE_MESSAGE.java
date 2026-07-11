package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 扩音器（喇叭）消息服务端包。
 * Server packet that delivers a megaphone/shout-item message to the client.
 *
 * @author (Encom)
 */
public class SM_MEGAPHONE_MESSAGE extends AionServerPacket {
	private Player player;
	private String message;
	private int itemId;
	private boolean isAll;

	/**
	 * 构造扩音器消息包。
	 * Builds a megaphone message packet.
	 *
	 * sender player
	 * message body
	 * @param itemId 使用的喇叭道具 ID / megaphone item template id
	 * @param isAll 是否全服广播（否则按种族过滤） / whether to broadcast to all races
	 */
	public SM_MEGAPHONE_MESSAGE(Player player, String message, int itemId, boolean isAll) {
		this.player = player;
		this.message = message;
		this.itemId = itemId;
		this.isAll = isAll;
	}

	@Override
	protected void writeImpl(AionConnection client) {
		writeS(player.getName());
		writeS(message);
		writeD(itemId);
		writeC(this.isAll ? this.player.getRace().getRaceId() : 255);
	}
}
