package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;
import com.aionemu.gameserver.model.house.PlayerScript;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送房屋脚本（压缩字节区间）数据的服务端包。
 * Server packet that sends house script data (a compressed-byte range) to the client.
 */
public class SM_HOUSE_SCRIPTS extends AionServerPacket {

	private int address;
	private PlayerScripts scripts;
	int from;
	int to;

	/**
	 * 使用房屋地址、脚本集合及闭区间 [from, to] 构造脚本同步包。
	 * Creates a script sync packet for the given house address, script set, and inclusive [from, to] range.
	 *
	 * house address id
	 * @param scripts 玩家脚本集合 / player scripts collection
	 * @param from 起始脚本槽位（含） / first script slot (inclusive)
	 * @param to 结束脚本槽位（含） / last script slot (inclusive)
	 */
	public SM_HOUSE_SCRIPTS(int address, PlayerScripts scripts, int from, int to) {
		this.address = address;
		this.scripts = scripts;
		this.from = from;
		this.to = to;
	}

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
