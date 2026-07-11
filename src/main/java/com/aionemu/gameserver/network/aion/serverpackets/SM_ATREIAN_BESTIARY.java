package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步单条阿特雷亚图鉴条目（击杀数、等级、可领奖状态）的服务端包。
 * Server packet synchronizing a single Atreian Bestiary entry (kill count, level, reward claimability).
 *
 * @author Ranastic
 */
public class SM_ATREIAN_BESTIARY extends AionServerPacket {

	private int id;
	private int kill;
	private int isRewardable;
	private byte level;

	/**
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @param kill 当前击杀数 / current kill count
	 * @param level 当前等级 / current level
	 * @param isRewardable 是否可领奖 / whether a reward can be claimed
	 */
	public SM_ATREIAN_BESTIARY(int id, int kill, byte level, int isRewardable) {
		this.id = id;
		this.kill = kill;
		this.isRewardable = isRewardable;
		this.level = level;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(id);
		writeD(kill);
		writeC(isRewardable);
		writeC(level);
	}
}
