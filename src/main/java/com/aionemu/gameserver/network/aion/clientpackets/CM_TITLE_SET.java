package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端设置当前显示称号请求包。
 * Client packet for setting the player's displayed title.
 *
 * @author Ranastic (Encom)
 */
public class CM_TITLE_SET extends AionClientPacket {
	private int titleId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TITLE_SET(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		titleId = readH();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (titleId != 0xFFFF) {
			if (!player.getTitleList().contains(titleId)
					&& !player.havePermission(MembershipConfig.TITLES_ADDITIONAL_ENABLE)) {
				return;
			}
		}
		player.getTitleList().setDisplayTitle(titleId);
	}
}
