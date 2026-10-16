package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 同步单条阿特雷亚图鉴条目（击杀数、阶段、已领奖数）的服务端包（0x15C / S_UPDATE_MONSTER_ARCHIVEMENT）。
 * Server packet synchronizing a single Atreian Bestiary entry (0x15C / S_UPDATE_MONSTER_ARCHIVEMENT).
 * <p>真端包序 {@code _SendUpdatedToClient}：id(D)、count(D)、grade(C)、receivedReward(C)。</p>
 * @author Ranastic
 */
public class SM_ATREIAN_BESTIARY extends AionServerPacket {

	private final int id;
	private final int kill;
	private final int grade;
	private final int receivedReward;

	/**
	 * @param id 图鉴条目 ID / bestiary entry id
	 * @param kill 当前击杀数 / current kill count
	 * @param grade 当前阶段（达标自动 +1，0..5）/ current grade (auto-incremented on conditions)
	 * @param receivedReward 已领取奖励的阶段数 / claimed reward count (retail receivedReward)
	 */
	public SM_ATREIAN_BESTIARY(int id, int kill, int grade, int receivedReward) {
		this.id = id;
		this.kill = kill;
		this.grade = grade;
		this.receivedReward = receivedReward;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(id);
		writeD(kill);
		writeC(grade);
		writeC(receivedReward);
	}
}
