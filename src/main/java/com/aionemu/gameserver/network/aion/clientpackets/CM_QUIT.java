package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUIT_RESPONSE;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.services.player.PlayerLeaveWorldService;

/**
 * 客户端退出/返回角色选择请求包。
 * Client packet to quit the game or return to character selection.
 */
public class CM_QUIT extends AionClientPacket {
	private boolean logout;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_QUIT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		logout = readC() == 1;
	}

	@Override
	protected void runImpl() {
		AionConnection client = getConnection();
		Player player = null;
		if (client.getState() == State.IN_GAME) {
			player = client.getActivePlayer();
			if (!logout) {
				com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().aionClientDisconnected(client.getAccount().getId());
			}
			PlayerLeaveWorldService.startLeaveWorld(player);
			client.setActivePlayer(null);
		}
		if (logout) {
			if (player != null && player.isInEditMode()) {
				sendPacket(new SM_QUIT_RESPONSE(true));
				player.setEditMode(false);
			} else {
				sendPacket(new SM_QUIT_RESPONSE());
			}
		} else {
			client.close(new SM_QUIT_RESPONSE(), false);
		}
	}
}
