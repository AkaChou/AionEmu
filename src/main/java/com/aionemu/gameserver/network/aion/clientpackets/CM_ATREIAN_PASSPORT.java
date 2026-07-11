package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameEventBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 领取阿特雷亚通行证奖励的客户端包。
 * Client packet to claim Atreian Passport rewards.
 *
 * @author Ghostfur (Aion-Unique)
 */
public class CM_ATREIAN_PASSPORT extends AionClientPacket {

	private int passportId;

	/**
	 * @param opcode
	 * @param state
	 * @param restStates
	 */
	public CM_ATREIAN_PASSPORT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aionemu.commons.network.packet.BaseClientPacket#readImpl()
	 */
	@Override
	protected void readImpl() {
		passportId = readD();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.aionemu.commons.network.packet.BaseClientPacket#runImpl()
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		GameEventBootstrapServices.atreianPassportService().getReward(player, passportId);
	}
}
