package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 玩家宏列表的服务端包。
 * Server packet that delivers a player's macro list.
 *
 * @author -Nemesiss-
 */
public class SM_MACRO_LIST extends AionServerPacket {

	private Player player;
	private int packet;

	/**
	 * 构造指定分片的宏列表包。
	 * Builds a macro-list packet for the given list part.
	 *
	 * target player
	 * @param packet 宏列表分片序号 / macro list part index
	 */
	public SM_MACRO_LIST(Player player, int packet) {
		this.player = player;
		this.packet = packet;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(player.getObjectId());// player id

		Map<Integer, String> macrosToSend = player.getMacroList().getMarcosPart(packet);

		int size = macrosToSend.size();

		if (packet == 1) {
			writeC(1);
		} else {
			writeC(0);
			size *= -1;
		}

		writeH(size);

		if (size != 0) {
			for (Map.Entry<Integer, String> entry : macrosToSend.entrySet()) {
				writeC(entry.getKey());// order
				writeS(entry.getValue());// xml
			}
		}
	}
}
