package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 请求欧比斯登陆信息的客户端包。
 * Client packet requesting abyss landing information.
 *
 * @author Ranastic & Lightning (Encom)
 */
@Slf4j

public class CM_ABYSS_LANDING extends AionClientPacket {

	public CM_ABYSS_LANDING(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameLocationBootstrapServices.abyssLandingService().sendPacketToPlayer(player);
	}
}
