package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SECURITY_TOKEN;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端请求账号安全令牌包。
 * Client packet for requesting the account security token.
 *
 * @author Falke_34, CoolyT
 */
public class CM_SECURITY_TOKEN extends AionClientPacket {

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_SECURITY_TOKEN(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		// 空 / empty
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		Account acc = getConnection().getAccount();
		if (acc.getSecurityToken() == null || acc.getSecurityToken().isEmpty()) {
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_SECURITY_TOKEN(acc.getSecurityToken()));
	}
}
