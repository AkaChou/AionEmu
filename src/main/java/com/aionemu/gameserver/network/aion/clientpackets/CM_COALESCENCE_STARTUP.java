package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_COALESCENCE_STARTUP;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 请求打开装备融合界面的客户端包。
 * Client packet requesting the equipment coalescence UI startup.
 *
 * @author Ranastic
 */
public class CM_COALESCENCE_STARTUP extends AionClientPacket {

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_COALESCENCE_STARTUP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		// 空 / null
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null || !player.isSpawned()) {
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_COALESCENCE_STARTUP(0));
	}
}