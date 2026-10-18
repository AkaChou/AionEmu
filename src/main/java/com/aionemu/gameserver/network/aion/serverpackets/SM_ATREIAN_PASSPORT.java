package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步阿特雷亚护照（月签到）状态的服务端包。
 * Server packet synchronizing Atreian Passport (monthly stamp) status to the client.
 * @author Rinzler (Encom)
 */
public class SM_ATREIAN_PASSPORT extends AionServerPacket {

	/**
	 * 单条签到记录。
	 * A single attendance record.
	 * @param passportId 签到 ID / passport id
	 * @param countCollected 已集印花数 / number of stamps collected
	 * @param attendType 签到类型（真端枚举：0=anniversary, 1=basic, 2=pc_basic）/ attendance type
	 * @param hasCollected 今日是否已领取 / whether today's stamp was already collected
	 */
	public record Record(int passportId, int countCollected, int attendType, boolean hasCollected) {
	}

	private final List<Record> records;
	private final int day;
	private final int month;
	private final int year;

	/**
	 * 以多条记录构造：真端 S_LOGIN_EVENT_LIST 一次下发全部签到。
	 * Builds the packet from multiple records: retail S_LOGIN_EVENT_LIST sends every attendance at once.
	 */
	public SM_ATREIAN_PASSPORT(List<Record> records, int day, int month, int year) {
		this.records = List.copyOf(records);
		this.day = day;
		this.month = month;
		this.year = year;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		// 真端 S_LOGIN_EVENT_LIST 布局：H 年, H 月, H 日, H 记录数, 记录数 × { D 事件ID, D 签到类型, D 已集印花数, C 今日已领 }
		// Retail S_LOGIN_EVENT_LIST layout: H year, H month, H day, H recordCount, records × { D eventId, D attendType, D stamps, C claimedToday }
		writeH(year);
		writeH(month);
		writeH(day);
		writeH(records.size());
		for (Record record : records) {
			writeD(record.passportId());
			writeD(record.attendType());
			writeD(record.countCollected());
			writeC(record.hasCollected() ? 0 : 1);
		}
	}
}
