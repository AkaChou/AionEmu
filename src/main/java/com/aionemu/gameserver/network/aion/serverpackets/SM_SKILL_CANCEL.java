package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端取消生物正在施放的技能。
 * Server packet notifying the client to cancel a creature's casting skill.
 *
 * @author Sweetkr
 */
@AllArgsConstructor
public class SM_SKILL_CANCEL extends AionServerPacket {

	private final Creature creature;
	private final int skillId;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(creature.getObjectId());
		writeH(skillId);
	}
}
