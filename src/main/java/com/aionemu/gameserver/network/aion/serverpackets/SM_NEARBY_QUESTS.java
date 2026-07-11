package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.HashMap;
import java.util.Map.Entry;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 附近任务列表服务端包。
 * Server packet that delivers the list of nearby quests to the client.
 * <p>
 * 映射 key 为任务 ID，value 为状态标志（&gt;0 时额外写入状态短整型）。
 * Map key is quest id; value is a status flag (&gt;0 writes an extra status short).
 */
public class SM_NEARBY_QUESTS extends AionServerPacket {
	private HashMap<Integer, Integer> nearbyQuestList;

	/**
	 * 构造附近任务列表包。
	 * Builds a nearby-quests list packet.
	 *
	 * @param nearbyQuestList 任务 ID → 状态标志映射 / quest id → status flag map
	 */
	public SM_NEARBY_QUESTS(HashMap<Integer, Integer> nearbyQuestList) {
		this.nearbyQuestList = nearbyQuestList;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		if (nearbyQuestList == null || con.getActivePlayer() == null) {
			return;
		}
		writeC(0);
		writeH(-nearbyQuestList.size() & 0xFFFF);
		for (Entry<Integer, Integer> nearbyQuest : nearbyQuestList.entrySet()) {
			if (nearbyQuest.getValue() > 0) {
				writeH(nearbyQuest.getKey());
				writeH(0x02);
			} else {
				writeD(nearbyQuest.getKey());
			}
		}
	}
}
