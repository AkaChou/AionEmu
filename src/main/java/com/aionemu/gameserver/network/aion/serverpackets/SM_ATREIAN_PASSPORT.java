package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步阿特雷亚护照（月签到）状态的服务端包。
 * Server packet synchronizing Atreian Passport (monthly stamp) status to the client.
 * @author Rinzler (Encom)
 */
public class SM_ATREIAN_PASSPORT extends AionServerPacket {

	private final int month;
	private final int year;
	private final int day;
	private final int passportId;
	private final int countCollected;
	private final int attendType;
	private final boolean hasCollected;

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
		// 真端 S_LOGIN_EVENT_LIST 布局：H 年, H 月, H 日, H 记录数, 记录数 × { D 事件ID, D 签到类型, D 已集印花数, C 今日已领 }
		// Retail S_LOGIN_EVENT_LIST layout: H year, H month, H day, H recordCount, records × { D eventId, D attendType, D stamps, C claimedToday }
		writeH(year);
		writeH(month);
		writeH(day);
		writeH(1); // 记录数，当前固定单条 / record count, currently a single record
		writeD(passportId);
		writeD(attendType); // 固定下发 1（客户端 ANNIVERSARY 渲染路径），见 AtreianPassportService / always 1, see AtreianPassportService
		writeD(countCollected);
		writeC(hasCollected ? 0 : 1);
	}
}
