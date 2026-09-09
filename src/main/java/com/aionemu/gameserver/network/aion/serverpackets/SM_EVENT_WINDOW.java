package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 活动窗口开关包：同步活动窗口是否激活及活跃活动数量。
 * Event-window toggle packet: active flag and active event count.
 *
 * @author Falke_34
 */
@AllArgsConstructor
public class SM_EVENT_WINDOW extends AionServerPacket {

	private final int active;
	private final int activeEventCount;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(active);
		writeC(activeEventCount);
	}
}
