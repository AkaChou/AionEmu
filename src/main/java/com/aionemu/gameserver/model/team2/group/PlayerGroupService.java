package com.aionemu.gameserver.model.team2.group;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.callbacks.metadata.GlobalCallback;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.model.bonus_service.ServiceBuff;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.common.events.PlayerLeavedEvent.LeaveReson;
import com.aionemu.gameserver.model.team2.common.events.ShowBrandEvent;
import com.aionemu.gameserver.model.team2.common.events.TeamKinahDistributionEvent;
import com.aionemu.gameserver.model.team2.common.legacy.GroupEvent;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.model.team2.group.callback.AddPlayerToGroupCallback;
import com.aionemu.gameserver.model.team2.group.callback.PlayerGroupCreateCallback;
import com.aionemu.gameserver.model.team2.group.callback.PlayerGroupDisbandCallback;
import com.aionemu.gameserver.model.team2.group.events.ChangeGroupLeaderEvent;
import com.aionemu.gameserver.model.team2.group.events.ChangeGroupLootRulesEvent;
import com.aionemu.gameserver.model.team2.group.events.GroupDisbandEvent;
import com.aionemu.gameserver.model.team2.group.events.PlayerConnectedEvent;
import com.aionemu.gameserver.model.team2.group.events.PlayerDisconnectedEvent;
import com.aionemu.gameserver.model.team2.group.events.PlayerEnteredEvent;
import com.aionemu.gameserver.model.team2.group.events.PlayerGroupInvite;
import com.aionemu.gameserver.model.team2.group.events.PlayerGroupLeavedEvent;
import com.aionemu.gameserver.model.team2.group.events.PlayerGroupStopMentoringEvent;
import com.aionemu.gameserver.model.team2.group.events.PlayerGroupUpdateEvent;
import com.aionemu.gameserver.model.team2.group.events.PlayerStartMentoringEvent;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.TimeUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

import java.util.LinkedHashMap;

/**
 * 玩家队伍服务，用于团队2相关逻辑。
 * Player Group Service for team 2 logic.
 */
@Slf4j

public class PlayerGroupService {
	private static final Map<Integer, PlayerGroup> groups = new ConcurrentHashMap<Integer, PlayerGroup>();
	private static final AtomicBoolean offlineCheckStarted = new AtomicBoolean();

	/** 邀请小队 / Invite To Group*/
	public static final void inviteToGroup(final Player inviter, final Player invited) {
		if (canInvite(inviter, invited)) {
			PlayerGroupInvite invite = new PlayerGroupInvite(inviter, invited);
			if (invited.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_PARTY_DO_YOU_ACCEPT_INVITATION,
					invite)) {
				PacketSendUtility.sendPacket(invited, new SM_QUESTION_WINDOW(
						SM_QUESTION_WINDOW.STR_PARTY_DO_YOU_ACCEPT_INVITATION, 0, 0, inviter.getName()));
			}
		}
	}

	/** 是否邀请 / Whether invite*/
	public static final boolean canInvite(Player inviter, Player invited) {
		if (inviter.isInInstance()) {
			if (GameCoreGameplayServices.autoGroupService().isAutoInstance(inviter.getInstanceId())) {
				// 在此区域无法使用与小队或联盟相关的邀请、离开或踢出命令。 / You cannot use invite, leave or kick commands related to your group or alliance in this region.
				PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_CANT_OPERATE_PARTY_COMMAND);
				return false;
			}
		}
		if (invited.isInInstance()) {
			if (GameCoreGameplayServices.autoGroupService().isAutoInstance(invited.getInstanceId())) {
				// 在此区域无法使用与小队或联盟相关的邀请、离开或踢出命令。 / You cannot use invite, leave or kick commands related to your group or alliance in this region.
				PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_CANT_OPERATE_PARTY_COMMAND);
				return false;
			}
		}
		PlayerGroup group = inviter.getPlayerGroup2();
		if (group != null) {
			if (invited.isInTeam()) {
				for (Player pm : invited.getCurrentTeam().getMembers()) {
					if (pm.isInInstance()) {
						// 因对方为小队长，且该玩家处于副本中，无法邀请其加入战团。 / You cannot invite the player to the force as the group leader of the player is in an Instanced Zone.
						PacketSendUtility.sendPacket(inviter, new SM_SYSTEM_MESSAGE(1400128));
						return false;
					}
				}
			}
		}
		return RestrictionsManager.canInviteToGroup(inviter, invited);
	}

	@GlobalCallback(PlayerGroupCreateCallback.class)
	/** 创建队伍。 / Create group. */
	public static final PlayerGroup createGroup(Player leader, Player invited, TeamType type) {
		PlayerGroup newGroup = new PlayerGroup(new PlayerGroupMember(leader), type);
		groups.put(newGroup.getTeamId(), newGroup);
		addPlayer(newGroup, leader);
		addPlayer(newGroup, invited);
		if (newGroup.getBuffId() != 0) {
			checkGroupBonus(leader, false);
			checkGroupBonus(invited, false);
		}
		newGroup.setBuffId(5 + newGroup.getMembers().size());
		checkGroupBonus(leader, true);
		checkGroupBonus(invited, true);
		if (offlineCheckStarted.compareAndSet(false, true)) {
			initializeOfflineCheck();
		}
		return newGroup;
	}

	/** 创建队伍。 / Create group. */
	public static final PlayerGroup createGroup(Player leader) {
		return createGroup(leader, TeamType.GROUP);
	}

	@GlobalCallback(PlayerGroupCreateCallback.class)
	/** 创建指定协议类型的单人队伍。 / Creates a single-player group with the given team type. */
	public static final PlayerGroup createGroup(Player leader, TeamType type) {
		PlayerGroup newGroup = new PlayerGroup(new PlayerGroupMember(leader), type);
		groups.put(newGroup.getTeamId(), newGroup);
		addPlayer(newGroup, leader);
		if (offlineCheckStarted.compareAndSet(false, true)) {
			initializeOfflineCheck();
		}
		return newGroup;
	}

	private static void initializeOfflineCheck() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new OfflinePlayerChecker(), 1000, 30 * 1000);
	}

	@GlobalCallback(AddPlayerToGroupCallback.class)
	/** 添加玩家到小队 / Adds player to group*/
	public static final void addPlayerToGroup(PlayerGroup group, Player invited) {
		group.addMember(new PlayerGroupMember(invited));
	}

	/** Change 小队 Rules / Change Group Rules */
	public static final void changeGroupRules(PlayerGroup group, LootGroupRules lootRules) {
		group.onEvent(new ChangeGroupLootRulesEvent(group, lootRules));
	}

	/** 玩家登录 / On Player Login */
	public static final void onPlayerLogin(Player player) {
		for (PlayerGroup group : groups.values()) {
			PlayerGroupMember member = group.getMember(player.getObjectId());
			if (member != null) {
				group.onEvent(new PlayerConnectedEvent(group, player));
			}
		}
	}

	/** 玩家登出时 / On Player Logout */
	public static final void onPlayerLogout(Player player) {
		PlayerGroup group = player.getPlayerGroup2();
		if (group != null) {
			PlayerGroupMember member = group.getMember(player.getObjectId());
			member.updateLastOnlineTime();
			group.onEvent(new PlayerDisconnectedEvent(group, player));
		}
	}

	/** 更新队伍。 / Update group. */
	public static final void updateGroup(Player player, GroupEvent groupEvent) {
		PlayerGroup group = player.getPlayerGroup2();
		if (group != null) {
			group.onEvent(new PlayerGroupUpdateEvent(group, player, groupEvent));
		}
	}

	/** 添加玩家。 / Adds player. */
	public static final void addPlayer(PlayerGroup group, Player player) {
		Preconditions.checkNotNull(group, "Group should not be null");
		group.onEvent(new PlayerEnteredEvent(group, player));
	}

	/** 移除玩家。 / Removes player. */
	public static final void removePlayer(Player player) {
		PlayerGroup group = player.getPlayerGroup2();
		if (group != null) {
			group.onEvent(new PlayerGroupLeavedEvent(group, player));
		}
	}

	/**
	 * 服务增益 5.5。
	 * Service Bonus 5.5.
	 */
	public static void checkGroupBonus(Player player, boolean add) {
		ServiceBuff serviceBuff;
		int buffId = player.getPlayerGroup2().getBuffId();
		if (add) {
			serviceBuff = new ServiceBuff(buffId);
			serviceBuff.applyEffect(player, buffId);
			serviceBuff.applyEffect(player, buffId);
		} else {
			serviceBuff = new ServiceBuff(buffId);
			serviceBuff.endEffect(player, buffId);
			serviceBuff.endEffect(player, buffId);
		}
	}

	/** 封禁玩家。 / Ban Player. */
	public static final void banPlayer(Player bannedPlayer, Player banGiver) {
		Preconditions.checkNotNull(bannedPlayer, "Banned player should not be null");
		Preconditions.checkNotNull(banGiver, "Bangiver player should not be null");
		PlayerGroup group = banGiver.getPlayerGroup2();
		if (group != null) {
			if (group.hasMember(bannedPlayer.getObjectId())) {
				group.onEvent(new PlayerGroupLeavedEvent(group, bannedPlayer, LeaveReson.BAN, banGiver.getName()));
			} else {
				log.warn(I18n.get("log.bb1e71bead26", group.onlineMembers()));
			}
		}
	}

	@GlobalCallback(PlayerGroupDisbandCallback.class)
	/** 解散 / disband. */
	public static void disband(PlayerGroup group) {
		Preconditions.checkState(group.onlineMembers() <= 1, "Can't disband group with more than one online member");
		groups.remove(group.getTeamId());
		group.onEvent(new GroupDisbandEvent(group));
	}

	/** 分配基纳 / Distribute Kinah */
	public static void distributeKinah(Player player, long kinah) {
		PlayerGroup group = player.getPlayerGroup2();
		if (group != null) {
			group.onEvent(new TeamKinahDistributionEvent<PlayerGroup>(group, player, kinah));
		}
	}

	/** 显示烙印标记 / show Brand. */
	public static void showBrand(Player player, int targetObjId, int brandId) {
		PlayerGroup group = player.getPlayerGroup2();
		if (group != null) {
			group.onEvent(new ShowBrandEvent<PlayerGroup>(group, targetObjId, brandId));
		}
	}

	/** 更换队长 / change Leader. */
	public static void changeLeader(Player player) {
		PlayerGroup group = player.getPlayerGroup2();
		if (group != null) {
			group.onEvent(new ChangeGroupLeaderEvent(group, player));
		}
	}

	/** Start mentoring / Start mentoring */
	public static void startMentoring(Player player) {
		PlayerGroup group = player.getPlayerGroup2();
		if (group != null) {
			group.onEvent(new PlayerStartMentoringEvent(group, player));
		}
	}

	/** Stop mentoring / Stop mentoring */
	public static void stopMentoring(Player player) {
		PlayerGroup group = player.getPlayerGroup2();
		if (group != null) {
			group.onEvent(new PlayerGroupStopMentoringEvent(group, player));
		}
	}

	/** 清理 / cleanup. */
	public static final void cleanup() {
		log.info(getServiceStatus());
		groups.clear();
	}

	/** 获取服务状态。 / Returns the service status. */
	public static final String getServiceStatus() {
		return I18n.get("log.5a64a545d768", groups.size());
	}

	/** 搜索队伍。 / Search group. */
	public static final PlayerGroup searchGroup(Integer playerObjId) {
		for (PlayerGroup group : groups.values()) {
			if (group.hasMember(playerObjId)) {
				return group;
			}
		}
		return null;
	}

	public static class OfflinePlayerChecker implements Runnable, Predicate<PlayerGroupMember> {
		private PlayerGroup currentGroup;

		/** 运行 / run. */
		@Override
		public void run() {
			for (PlayerGroup group : groups.values()) {
				currentGroup = group;
				group.apply(this);
			}
			currentGroup = null;
		}

		/** 应用。 / Apply. */
		@Override
		public boolean apply(PlayerGroupMember member) {
			if (!member.isOnline()
					&& TimeUtil.isExpired(member.getLastOnlineTime() + GroupConfig.GROUP_REMOVE_TIME * 1000)) {
				currentGroup.onEvent(new PlayerGroupLeavedEvent(currentGroup, member.getObject()));
			}
			return true;
		}
	}
}
