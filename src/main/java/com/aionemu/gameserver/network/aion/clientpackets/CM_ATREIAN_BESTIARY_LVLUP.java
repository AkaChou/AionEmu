package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.player.AtreianBestiaryService;

/**
 * 阿特雷亚图鉴条目升级的客户端包。
 * Client packet to level up an Atreian Bestiary entry.
 *
 * @author Ranastic
 */
public class CM_ATREIAN_BESTIARY_LVLUP extends AionClientPacket {
	private int id;

	public CM_ATREIAN_BESTIARY_LVLUP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		id = readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		GameFeatureServices.atreianBestiaryService().onLvlUp(player, id);
	}
}
