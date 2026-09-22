package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端移除指定技能。
 * Server packet notifying the client to remove a skill.
 * @author xTz
 */
@AllArgsConstructor
public class SM_SKILL_REMOVE extends AionServerPacket {

	private final int skillId;
	private final int skillLevel;
	private final boolean isStigma;
	private final boolean isLinked;

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
