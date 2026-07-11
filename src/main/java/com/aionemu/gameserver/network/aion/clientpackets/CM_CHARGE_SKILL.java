package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 停止充能技能蓄力的客户端包。
 * Client packet to stop a charging skill cast.
 *
 * @author Dr.Nism
 */
public class CM_CHARGE_SKILL extends AionClientPacket {

	public CM_CHARGE_SKILL(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		// 空 / empty
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		if (player.isCasting()) {
			player.getCastingSkill().stopCharging();
		}
	}
}
