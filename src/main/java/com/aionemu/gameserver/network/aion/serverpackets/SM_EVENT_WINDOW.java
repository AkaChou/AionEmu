package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 活动窗口开关包：同步活动窗口是否激活及活跃活动数量。
 * Event-window toggle packet: active flag and active event count.
 *
 * @author Falke_34
 */
public class SM_EVENT_WINDOW extends AionServerPacket {

	private int active;
	private int activeEventCount;

	/**
	 * 构造活动窗口开关包。
	 * Creates an event-window toggle packet.
	 *
	 * @param active 活动窗口是否激活 / whether the event window is active
	 * @param activeEventCount 活跃活动数量 / number of active events
	 */
	public SM_EVENT_WINDOW(int active, int activeEventCount) {
		this.active = active;
		this.activeEventCount = activeEventCount;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(active);
		writeC(activeEventCount);
	}
}
