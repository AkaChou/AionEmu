package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端展示复活选项（复活者与技能信息）。
 * Server packet presenting resurrection options (resurrector and skill info) to the client.
 *
 * @author ATracer
 * @author Jego
 */
public class SM_RESURRECT extends AionServerPacket {

	private String name;
	private int skillId;

	/**
	 * 使用给定参数构造 SM_RESURRECT 包。
	 * Creates a SM_RESURRECT packet with the given parameters.
	 *
	 * creature
	 */
	public SM_RESURRECT(Creature creature) {
		this(creature, 0);
	}

	/**
	 * 使用给定参数构造 SM_RESURRECT 包。
	 * Creates a SM_RESURRECT packet with the given parameters.
	 *
	 * creature
	 * skill id
	 */
	public SM_RESURRECT(Creature creature, int skillId) {
		this.name = creature.getName();
		this.skillId = skillId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeS(name);
		writeH(skillId); // 未知 / unk
		writeD(0);
	}
}
