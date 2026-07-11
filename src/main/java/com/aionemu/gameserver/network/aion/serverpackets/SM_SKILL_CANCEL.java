package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端取消生物正在施放的技能。
 * Server packet notifying the client to cancel a creature's casting skill.
 *
 * @author Sweetkr
 */
public class SM_SKILL_CANCEL extends AionServerPacket {

	private Creature creature;
	private int skillId;

	/**
	 * 使用给定参数构造 SM_SKILL_CANCEL 包。
	 * Creates a SM_SKILL_CANCEL packet with the given parameters.
	 *
	 * creature
	 * skill id
	 */
	public SM_SKILL_CANCEL(Creature creature, int skillId) {
		this.creature = creature;
		this.skillId = skillId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(creature.getObjectId());
		writeH(skillId);
	}
}
