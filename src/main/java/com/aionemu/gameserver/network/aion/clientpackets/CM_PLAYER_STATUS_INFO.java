package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.common.events.TeamCommand;
import com.aionemu.gameserver.model.team2.common.service.PlayerTeamCommandService;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * 客户端队伍/联盟状态与管理指令包，进世界及组队管理时发送。
 * Client packet for party/alliance status and management commands, sent on enter-world and team ops.
 *
 * @author Lyahim, ATracer, Simple, xTz
 */
public class CM_PLAYER_STATUS_INFO extends AionClientPacket {

	private int commandCode;
	private int playerObjId;
	private int allianceGroupId;
	private int secondObjectId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_PLAYER_STATUS_INFO(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		commandCode = readC();
		playerObjId = readD();
		allianceGroupId = readD();
		secondObjectId = readD();

	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		TeamCommand command = TeamCommand.getCommand(commandCode);
		switch (command) {
		case GROUP_SET_LFG:
			activePlayer.setLookingForGroup(playerObjId == 2);
			break;
		case ALLIANCE_CHANGE_GROUP:
			PlayerAllianceService.changeMemberGroup(activePlayer, playerObjId, secondObjectId, allianceGroupId);
			break;
		default:
			PlayerTeamCommandService.executeCommand(activePlayer, command, playerObjId);
		}
	}
}
