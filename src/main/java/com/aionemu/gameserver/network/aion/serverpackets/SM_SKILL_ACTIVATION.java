package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步技能激活/切换状态。
 * Server packet synchronizing skill activation/toggle state to the client.
 *
 * @author Sweetkr
 */
public class SM_SKILL_ACTIVATION extends AionServerPacket {

	private boolean isActive;
	private int unk;
	private int skillId;

	/**
	 * 使用给定参数构造 SM_SKILL_ACTIVATION 包。
	 * For toggle skills
	 *
	 * skill id
	 * active flag
	 */
	public SM_SKILL_ACTIVATION(int skillId, boolean isActive) {
		this.skillId = skillId;
		this.isActive = isActive;
		this.unk = 0;
	}

	/**
	 * 使用给定参数构造 SM_SKILL_ACTIVATION 包。
	 * For stigma remove should work in 1.5.1.15
	 *
	 * skill id
	 */
	public SM_SKILL_ACTIVATION(int skillId) {
		this.skillId = skillId;
		this.isActive = true;
		this.unk = 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeH(skillId);
		writeD(unk);
		writeC(isActive ? 1 : 0);
	}
}
