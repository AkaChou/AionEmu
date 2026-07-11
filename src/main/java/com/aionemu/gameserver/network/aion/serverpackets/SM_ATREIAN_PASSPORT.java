package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步阿特雷亚护照（月签到）状态的服务端包。
 * Server packet synchronizing Atreian Passport (monthly stamp) status to the client.
 *
 * @author Rinzler (Encom)
 */
public class SM_ATREIAN_PASSPORT extends AionServerPacket {

	private int month;
	private int year;
	private int day;
	private int passportId;
	private int countCollected;
	private int attendType;
	private boolean hasCollected;

	/**
	 * passport template id
	 * @param countCollected 已收集印花数 / number of stamps collected
	 * attendance type
	 * @param hasCollected 今日是否已领取 / whether today's stamp was already collected
	 * day
	 * month
	 * year
	 */
	public SM_ATREIAN_PASSPORT(int passportId, int countCollected, int attendType, boolean hasCollected,
			int day, int month, int year) {
		this.month = month;
		this.year = year;
		this.day = day;
		this.passportId = passportId;
		this.countCollected = countCollected;
		this.attendType = attendType;
		this.hasCollected = hasCollected;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(year);
		writeH(month);
		writeH(day);
		writeH(1);
		writeD(passportId);
		writeD(attendType);
		writeD(countCollected);
		writeC(hasCollected ? 0 : 1);
	}
}
