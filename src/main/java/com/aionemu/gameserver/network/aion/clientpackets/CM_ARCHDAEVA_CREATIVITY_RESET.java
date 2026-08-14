package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 请求重置高阶守护者创造力（Archdaeva Creativity）的客户端包。
 * Client packet requesting an Archdaeva creativity reset.
 *
 * @author Ranastic (Encom)
 */
@Slf4j

public class CM_ARCHDAEVA_CREATIVITY_RESET extends AionClientPacket {

	public CM_ARCHDAEVA_CREATIVITY_RESET(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		// 空字节 / empty byte
	}

	@Override
	protected void runImpl() {
		Player player = this.getConnection().getActivePlayer();
		log.info(I18n.get("log.7b80e9ea0a2e"));
	}
}
