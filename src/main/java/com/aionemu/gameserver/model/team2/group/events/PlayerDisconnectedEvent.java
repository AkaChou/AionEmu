package com.aionemu.gameserver.model.team2.group.events;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamEvent;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;

/**
 * 玩家断线事件（团队2）。
 * Player Disconnected Event for team 2 logic.
 *
 * @author ATracer
 */
public class PlayerDisconnectedEvent implements Predicate<Player>, TeamEvent {

	private final PlayerGroup group;
	private final Player player;

	public PlayerDisconnectedEvent(PlayerGroup group, Player player) {
		this.group = group;
		this.player = player;
	}

	/**
	 * 断线前玩家必须已在队伍中。
	 * Player should be in group before disconnection.
	 *
	 * @return 是否满足条件 / whether the condition holds
	 */
	@Override
	public boolean checkCondition() {
		return group.hasMember(player.getObjectId());
	}

	/** 处理事件。 / Handle event. */
	@Override
	public void handleEvent() {
		if (group.onlineMembers() <= 1) {
			PlayerGroupService.disband(group);
		} else {
			if (player.equals(group.getLeader().getObject())) {
				group.onEvent(new ChangeGroupLeaderEvent(group));
			}
			group.applyOnMembers(this);
		}
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player member) {
		if (!member.equals(player)) {
			PacketSendUtility.sendPacket(member, SM_SYSTEM_MESSAGE.STR_PARTY_HE_BECOME_OFFLINE(player.getName()));
			PacketSendUtility.sendPacket(member, new SM_GROUP_MEMBER_INFO(group, player, GroupEvent.DISCONNECTED));
			// 登出时是否断开其他队员？检查 / disconnect other group members on logout? check
			PacketSendUtility.sendPacket(player, new SM_GROUP_MEMBER_INFO(group, member, GroupEvent.DISCONNECTED));
		}
		return true;
	}
}
