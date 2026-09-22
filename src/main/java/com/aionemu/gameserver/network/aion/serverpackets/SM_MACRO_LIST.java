package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 玩家宏列表的服务端包。
 * Server packet that delivers a player's macro list.
 * @author -Nemesiss-
 */
@AllArgsConstructor
public class SM_MACRO_LIST extends AionServerPacket {

	private final Player player;
	private final int packet;

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
