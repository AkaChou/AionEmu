package com.aionemu.gameserver.model.team2.group.events;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.common.events.AlwaysTrueTeamEvent;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupMember;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GROUP_MEMBER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.google.common.base.Predicate;

/**
 * 玩家 Connected 活动，用于团队2相关逻辑。
 * Player Connected Event for team 2 logic.
 *
 * @author ATracer
 */
public class PlayerConnectedEvent extends AlwaysTrueTeamEvent implements Predicate<Player> {

	private final PlayerGroup group;
	private final Player player;

	public PlayerConnectedEvent(PlayerGroup group, Player player) {
		this.group = group;
		this.player = player;
	}

	/** 处理活动。 / Handle event. */
	@Override
	public void handleEvent() {
		group.removeMember(player.getObjectId());
		PlayerGroupMember member = new PlayerGroupMember(player);
		group.addMember(member);
		if (player.sameObjectId(group.getLeader().getObjectId())) {
			group.changeLeader(member);
		}
		PacketSendUtility.sendPacket(player, new SM_GROUP_INFO(group));
		PacketSendUtility.sendPacket(player, new SM_GROUP_MEMBER_INFO(group, player, GroupEvent.JOIN));
		group.applyOnMembers(this);
	}

	/** 应用。 / Apply. */
	@Override
	public boolean apply(Player member) {
		if (!player.equals(member)) {
			PacketSendUtility.sendPacket(member, new SM_GROUP_MEMBER_INFO(group, player, GroupEvent.ENTER));
			PacketSendUtility.sendPacket(member, new SM_INSTANCE_INFO(player, false, group));
			PacketSendUtility.sendPacket(player, new SM_GROUP_MEMBER_INFO(group, member, GroupEvent.ENTER));
		}
		return true;
	}
}
