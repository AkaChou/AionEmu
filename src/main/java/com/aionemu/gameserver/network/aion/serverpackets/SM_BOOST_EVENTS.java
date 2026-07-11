package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.model.templates.event.BoostEvents;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步增益/加成活动状态（buff 编号、加成值与活动起止时间）的服务端包。
 * Server packet synchronizing boost-event state (buff id, boost value, and event start/end times).
 *
 * @author wanke
 */
public class SM_BOOST_EVENTS extends AionServerPacket {
	private Map<Integer, BoostEvents> boostEvents;

	private int buffId;
	private int buffValue;
	long eventStartTime;
	long eventEndTime;

	/**
	 * 构造增益活动同步包。
	 * Builds a boost-event sync packet.
	 *
	 * boost buff id
	 * boost value
	 * @param eventStartTime 活动开始时间戳 / event start timestamp
	 * @param eventEndTime 活动结束时间戳 / event end timestamp
	 */
	public SM_BOOST_EVENTS(int buffId, int buffValue, long eventStartTime, long eventEndTime) {
		this.buffId = buffId;
		this.buffValue = buffValue;
		this.eventStartTime = eventStartTime;
		this.eventEndTime = eventEndTime;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(9); // buff Count dont tuch
		writeC(buffId); // buff Id
		writeC(1); // enabledCount
		writeD((int) eventStartTime); // start
		writeD(0);
		writeD((int) eventEndTime); // end
		writeD(0);
		writeD(buffValue); // boost value
		writeQ(-1);
		writeD(0);
		writeD(0);
	}
}
