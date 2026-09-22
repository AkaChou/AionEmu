package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端召唤物使用技能。
 * Server packet notifying the client that a summon is using a skill.
 * @author ATracer
 */
@AllArgsConstructor
public class SM_SUMMON_USESKILL extends AionServerPacket {

	private final int summonId;
	private final int skillId;
	private final int skillLvl;
	private final int targetId;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(summonId);
		writeH(skillId);
		writeC(skillLvl);
		writeD(targetId);
	}
}
