package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端移除指定技能。
 * Server packet notifying the client to remove a skill.
 *
 * @author xTz
 */
public class SM_SKILL_REMOVE extends AionServerPacket {

	private int skillId;
	private int skillLevel;
	private boolean isStigma;
	private boolean isLinked;

	/**
	 * 使用给定参数构造 SM_SKILL_REMOVE 包。
	 * Creates a SM_SKILL_REMOVE packet with the given parameters.
	 *
	 * skill id
	 * skill level
	 * is stigma
	 * is linked
	 */
	public SM_SKILL_REMOVE(int skillId, int skillLevel, boolean isStigma, boolean isLinked) {
		this.skillId = skillId;
		this.skillLevel = skillLevel;
		this.isStigma = isStigma;
		this.isLinked = isLinked;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeH(skillId);
		if (skillId >= 30001 && skillId <= 30003 || skillId >= 40001 && skillId <= 40010) {
			writeC(0);
			writeC(0);
		} else if (isStigma) {
			writeC(skillLevel);
			writeC(1);
		} else if (isLinked) {
			writeC(1);
			writeC(3);
		} else { // remove skills active or passive
			writeC(skillLevel);
		}
	}
}
