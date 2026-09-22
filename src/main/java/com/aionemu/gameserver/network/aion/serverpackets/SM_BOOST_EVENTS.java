package com.aionemu.gameserver.network.aion.serverpackets;


import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 同步增益/加成活动状态（buff 编号、加成值与活动起止时间）的服务端包。
 * Server packet synchronizing boost-event state (buff id, boost value, and event start/end times).
 * @author wanke
 */
@AllArgsConstructor
public class SM_BOOST_EVENTS extends AionServerPacket {

	private final int buffId;
	private final int buffValue;
	long eventStartTime;
	long eventEndTime;

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
