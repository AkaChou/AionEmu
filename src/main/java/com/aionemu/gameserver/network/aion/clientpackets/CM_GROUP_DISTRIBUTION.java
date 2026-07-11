package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.restrictions.RestrictionsManager;

/**
 * 在队伍/联盟中分配基纳的客户端包。
 * Client packet that distributes kinah within a party or alliance.
 *
 * @author Lyahim, Simple, xTz
 */
public class CM_GROUP_DISTRIBUTION extends AionClientPacket {

	private long amount;
	private int partyType;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_GROUP_DISTRIBUTION(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		amount = readQ();
		partyType = readC();
	}

	@Override
	protected void runImpl() {
		if (amount < 2) {
			return;
		}
		Player player = getConnection().getActivePlayer();

		if (!RestrictionsManager.canTrade(player)) {
			return;
		}
		switch (partyType) {
		case 1:
			if (player.isInAlliance2()) {
				PlayerAllianceService.distributeKinahInGroup(player, amount);
			} else {
				PlayerGroupService.distributeKinah(player, amount);
			}
			break;
		case 2:
			PlayerAllianceService.distributeKinah(player, amount);
			break;
		}
	}
}