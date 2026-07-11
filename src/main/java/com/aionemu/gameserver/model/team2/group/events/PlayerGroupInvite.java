package com.aionemu.gameserver.model.team2.group.events;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 玩家队伍 Invite，用于团队2相关逻辑。
 * Player Group Invite for team 2 logic.
 */

public class PlayerGroupInvite extends RequestResponseHandler {
	private final Player inviter;
	private final Player invited;

	public PlayerGroupInvite(Player inviter, Player invited) {
		super(inviter);
		this.inviter = inviter;
		this.invited = invited;
	}

	/** 接受请求 / Accept Request */
	@Override
	public void acceptRequest(Creature requester, Player responder) {
		if (PlayerGroupService.canInvite(inviter, invited)) {
			// 你已邀请 %0 加入小队。 / You have invited %0 to join your group.
			PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_PARTY_INVITED_HIM(invited.getName()));
			PlayerGroup group = inviter.getPlayerGroup2();
			if (group != null) {
				PlayerGroupService.addPlayer(group, invited);
			} else {
				PlayerGroupService.createGroup(inviter, invited, TeamType.GROUP);
			}
		}
	}

	/** 拒绝请求 / Deny Request */
	@Override
	public void denyRequest(Creature requester, Player responder) {
		// %0 拒绝了你的邀请。 / %0 has declined your invitation.
		PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_PARTY_HE_REJECT_INVITATION(responder.getName()));
	}
}
