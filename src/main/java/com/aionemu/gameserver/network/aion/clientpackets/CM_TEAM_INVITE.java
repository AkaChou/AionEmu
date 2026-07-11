package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.DeniedStatus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.team2.league.LeagueService;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.World;

/**
 * 客户端队伍/联盟/军团同盟邀请请求包。
 * Client packet for inviting a player to group, alliance, or league.
 *
 * @author Lyahim, ATracer Modified by Simple
 */
public class CM_TEAM_INVITE extends AionClientPacket {

	private String name;
	private int inviteType;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TEAM_INVITE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		inviteType = readC();
		name = readS();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {

		name = ChatUtil.getRealAdminName(name);
		name = name.replace("\uE024", "");
		name = name.replace("\uE023", "");
		final String playerName = Util.convertName(name);

		final Player inviter = getConnection().getActivePlayer();
		if (inviter.getLifeStats().isAlreadyDead()) {
			// 死亡时无法发出邀请。 / You cannot issue an invitation while you are dead.
			PacketSendUtility.sendPacket(inviter, new SM_SYSTEM_MESSAGE(1300163));
			return;
		}

		final Player invited = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerName);
		if (invited != null) {
			if (invited.getPlayerSettings().isInDeniedStatus(DeniedStatus.GROUP)) {
				sendPacket(SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_PARTY(invited.getName()));
				return;
			}
			switch (inviteType) {
			case 0:
				PlayerGroupService.inviteToGroup(inviter, invited);
				break;
			case 12: // 2.5
				PlayerAllianceService.inviteToAlliance(inviter, invited);
				break;
			case 28:
				LeagueService.inviteToLeague(inviter, invited);
				break;
			default:
				PacketSendUtility.sendMessage(inviter, "You used an unknown invite type: " + inviteType);
				break;
			}
		} else {
			inviter.getClientConnection().sendPacket(SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(name));
		}
	}
}
