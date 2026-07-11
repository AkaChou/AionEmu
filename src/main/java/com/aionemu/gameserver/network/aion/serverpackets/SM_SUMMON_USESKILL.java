package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端召唤物使用技能。
 * Server packet notifying the client that a summon is using a skill.
 *
 * @author ATracer
 */
public class SM_SUMMON_USESKILL extends AionServerPacket {

	private int summonId;
	private int skillId;
	private int skillLvl;
	private int targetId;

	/**
	 * 使用给定参数构造 SM_SUMMON_USESKILL 包。
	 * Creates a SM_SUMMON_USESKILL packet with the given parameters.
	 *
	 * summon id
	 * skill id
	 * skill level
	 * target id
	 */
	public SM_SUMMON_USESKILL(int summonId, int skillId, int skillLvl, int targetId) {
		this.summonId = summonId;
		this.skillId = skillId;
		this.skillLvl = skillLvl;
		this.targetId = targetId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(summonId);
		writeH(skillId);
		writeC(skillLvl);
		writeD(targetId);
	}
}
