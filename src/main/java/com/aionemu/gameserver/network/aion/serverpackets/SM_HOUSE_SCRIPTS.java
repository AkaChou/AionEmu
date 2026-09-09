package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;
import com.aionemu.gameserver.model.house.PlayerScript;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端发送房屋脚本（压缩字节区间）数据的服务端包。
 * Server packet that sends house script data (a compressed-byte range) to the client.
 */
@AllArgsConstructor
public class SM_HOUSE_SCRIPTS extends AionServerPacket {

	private final int address;
	private final PlayerScripts scripts;
	int from;
	int to;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(address);
		writeH(to - from + 1);
		Map<Integer, PlayerScript> scriptMap = scripts.getScripts();
		for (int position = from; position <= to; position++) {
			writeC(position);
			PlayerScript script = scriptMap.get(position);
			byte[] bytes = script.getCompressedBytes();
			if (bytes == null) {
				writeH(-1);
			} else if (bytes.length == 0) {
				writeH(0);
			} else {
				writeH(bytes.length + 8);
				writeD(bytes.length);
				writeD(script.getUncompressedSize());
				writeB(bytes);
			}
		}
	}
}
