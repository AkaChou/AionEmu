package com.aionemu.gameserver.model.team2.alliance;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.commons.callbacks.metadata.GlobalCallback;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.alliance.callback.AddPlayerToAllianceCallback;
import com.aionemu.gameserver.model.team2.alliance.callback.PlayerAllianceCreateCallback;
import com.aionemu.gameserver.model.team2.alliance.callback.PlayerAllianceDisbandCallback;
import com.aionemu.gameserver.model.team2.alliance.events.AllianceDisbandEvent;
import com.aionemu.gameserver.model.team2.alliance.events.AssignViceCaptainEvent;
import com.aionemu.gameserver.model.team2.alliance.events.AssignViceCaptainEvent.AssignType;
import com.aionemu.gameserver.model.team2.alliance.events.ChangeAllianceLeaderEvent;
import com.aionemu.gameserver.model.team2.alliance.events.ChangeAllianceLootRulesEvent;
import com.aionemu.gameserver.model.team2.alliance.events.ChangeMemberGroupEvent;
import com.aionemu.gameserver.model.team2.alliance.events.CheckAllianceReadyEvent;
import com.aionemu.gameserver.model.team2.alliance.events.PlayerAllianceInvite;
import com.aionemu.gameserver.model.team2.alliance.events.PlayerAllianceLeavedEvent;
import com.aionemu.gameserver.model.team2.alliance.events.PlayerAllianceUpdateEvent;
import com.aionemu.gameserver.model.team2.alliance.events.PlayerConnectedEvent;
import com.aionemu.gameserver.model.team2.alliance.events.PlayerDisconnectedEvent;
import com.aionemu.gameserver.model.team2.alliance.events.PlayerEnteredEvent;
import com.aionemu.gameserver.model.team2.common.events.PlayerLeavedEvent.LeaveReson;
import com.aionemu.gameserver.model.team2.common.events.ShowBrandEvent;
import com.aionemu.gameserver.model.team2.common.events.TeamCommand;
import com.aionemu.gameserver.model.team2.common.events.TeamKinahDistributionEvent;
import com.aionemu.gameserver.model.team2.common.legacy.LootGroupRules;
import com.aionemu.gameserver.model.team2.common.legacy.PlayerAllianceEvent;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.TimeUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * 玩家联盟服务，用于团队2相关逻辑。
 * Player Alliance Service for team 2 logic.
 */
@Slf4j

public class PlayerAllianceService {
	private static final Map<Integer, PlayerAlliance> alliances = new ConcurrentHashMap<Integer, PlayerAlliance>();
	private static final AtomicBoolean offlineCheckStarted = new AtomicBoolean();

	/** Invite 联盟 / Invite To Alliance */
	public static final void inviteToAlliance(final Player inviter, final Player invited) {
		if (canInvite(inviter, invited)) {
			PlayerAllianceInvite invite = new PlayerAllianceInvite(inviter, invited);
			if (invited.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_MSGBOX_FORCE_INVITE_PARTY, invite)) {
				if (invited.isInGroup2()) {
					PacketSendUtility.sendPacket(inviter,
							SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_INVITED_HIS_PARTY(invited.getName()));
				} else {
					PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_FORCE_INVITED_HIM(invited.getName()));
				}
				PacketSendUtility.sendPacket(invited, new SM_QUESTION_WINDOW(
						SM_QUESTION_WINDOW.STR_MSGBOX_FORCE_INVITE_PARTY, 0, 0, inviter.getName()));
			}
		}
	}

	/** 是否邀请 / Whether invite*/
	public static final boolean canInvite(Player inviter, Player invited) {
		if (inviter.isInInstance()) {
			if (GameCoreGameplayServices.autoGroupService().isAutoInstance(inviter.getInstanceId())) {
				// 在此区域无法使用与小队或 / You cannot use invite, leave or kick commands related to your group or
				// 联盟相关的邀请、离开或踢出命令。 / alliance in this region.
				PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_CANT_OPERATE_PARTY_COMMAND);
				return false;
			}
		}
		if (invited.isInInstance()) {
			if (GameCoreGameplayServices.autoGroupService().isAutoInstance(invited.getInstanceId())) {
				// 在此区域无法使用与小队或 / You cannot use invite, leave or kick commands related to your group or
				// 联盟相关的邀请、离开或踢出命令。 / alliance in this region.
				PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_CANT_OPERATE_PARTY_COMMAND);
				return false;
			}
		}
		PlayerAlliance alliance = inviter.getPlayerAlliance2();
		if (alliance != null && alliance.getTeamType().isDefence()) {
			if (invited.isInTeam()) {
				for (Player tm : invited.getCurrentTeam().getMembers()) {
					if (tm.isInInstance()) {
						// 因对方为小队长，无法邀请该玩家加入战团。 / You cannot invite the player to the force as the group leader of the player
						// 处于副本中。 / is in an Instanced Zone.
						PacketSendUtility.sendPacket(inviter, new SM_SYSTEM_MESSAGE(1400128));
						return false;
					} else if (!GameLocationBootstrapServices.vortexService().isInsideVortexZone(tm)) {
						PacketSendUtility.sendPacket(inviter, SM_SYSTEM_MESSAGE
								.STR_PARTY_ALLIANCE_CANT_INVITE_WHEN_HE_IS_ASKED_QUESTION(tm.getName()));
						return false;
					}
				}
			} else if (!GameLocationBootstrapServices.vortexService().isInsideVortexZone(invited)) {
				// 无法邀请不同区域的人。 / You cannot invite someone in a different area.
				PacketSendUtility.sendPacket(inviter, new SM_SYSTEM_MESSAGE(1401527));
				return false;
			}
		}
		return RestrictionsManager.canInviteToAlliance(inviter, invited);
	}

	@GlobalCallback(PlayerAllianceCreateCallback.class)
	/** 创建联盟。 / Create alliance. */
	public static final PlayerAlliance createAlliance(Player leader, Player invited, TeamType type) {
		PlayerAlliance newAlliance = new PlayerAlliance(new PlayerAllianceMember(leader), type);
		alliances.put(newAlliance.getTeamId(), newAlliance);
		addPlayer(newAlliance, leader);
		addPlayer(newAlliance, invited);
		if (offlineCheckStarted.compareAndSet(false, true)) {
			initializeOfflineCheck();
		}
		return newAlliance;
	}

	@GlobalCallback(PlayerAllianceCreateCallback.class)
	/** 创建指定协议类型的单人联盟。 / Creates a single-player alliance of the given team type. */
	public static final PlayerAlliance createAlliance(Player leader, TeamType type) {
		PlayerAlliance newAlliance = new PlayerAlliance(new PlayerAllianceMember(leader), type);
		alliances.put(newAlliance.getTeamId(), newAlliance);
		addPlayer(newAlliance, leader);
		if (offlineCheckStarted.compareAndSet(false, true)) {
			initializeOfflineCheck();
		}
		return newAlliance;
	}

	private static void initializeOfflineCheck() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new OfflinePlayerAllianceChecker(), 1000, 30 * 1000);
	}

	@GlobalCallback(AddPlayerToAllianceCallback.class)
	/** 添加玩家到联盟 / Adds player to alliance*/
	public static final void addPlayerToAlliance(PlayerAlliance alliance, Player invited) {
		alliance.addMember(new PlayerAllianceMember(invited));
	}

	/** 更改小队规则 / Change Group Rules */
	public static final void changeGroupRules(PlayerAlliance alliance, LootGroupRules lootRules) {
		alliance.onEvent(new ChangeAllianceLootRulesEvent(alliance, lootRules));
	}

	/** 玩家登录 / On Player Login */
	public static final void onPlayerLogin(Player player) {
		for (PlayerAlliance alliance : alliances.values()) {
			PlayerAllianceMember member = alliance.getMember(player.getObjectId());
			if (member != null) {
				alliance.onEvent(new PlayerConnectedEvent(alliance, player));
			}
		}
	}

	/** 玩家登出时 / On Player Logout */
	public static final void onPlayerLogout(Player player) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		if (alliance != null) {
			PlayerAllianceMember member = alliance.getMember(player.getObjectId());
			member.updateLastOnlineTime();
			alliance.onEvent(new PlayerDisconnectedEvent(alliance, player));
		}
	}

	/** 更新联盟。 / Update alliance. */
	public static final void updateAlliance(Player player, PlayerAllianceEvent allianceEvent) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		if (alliance != null) {
			alliance.onEvent(new PlayerAllianceUpdateEvent(alliance, player, allianceEvent));
		}
	}

	/** 添加玩家。 / Adds player. */
	public static final void addPlayer(PlayerAlliance alliance, Player player) {
		Preconditions.checkNotNull(alliance, "Alliance should not be null");
		alliance.onEvent(new PlayerEnteredEvent(alliance, player));
	}

	/** 移除玩家。 / Removes player. */
	public static final void removePlayer(Player player) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		if (alliance != null) {
			if (alliance.getTeamType().isDefence()) {
				GameLocationBootstrapServices.vortexService().removeDefenderPlayer(player);
			}
			alliance.onEvent(new PlayerAllianceLeavedEvent(alliance, player));
		}
	}

	/** 封禁玩家。 / Ban Player. */
	public static final void banPlayer(Player bannedPlayer, Player banGiver) {
		Preconditions.checkNotNull(bannedPlayer, "Banned player should not be null");
		Preconditions.checkNotNull(banGiver, "Bangiver player should not be null");
		PlayerAlliance alliance = banGiver.getPlayerAlliance2();
		if (alliance != null) {
			if (alliance.getTeamType().isDefence()) {
				GameLocationBootstrapServices.vortexService().removeDefenderPlayer(bannedPlayer);
			}
			PlayerAllianceMember bannedMember = alliance.getMember(bannedPlayer.getObjectId());
			if (bannedMember != null) {
				alliance.onEvent(new PlayerAllianceLeavedEvent(alliance, bannedMember.getObject(), LeaveReson.BAN,
						banGiver.getName()));
			} else {
				log.warn(I18n.get("log.937a517c5094", alliance.onlineMembers()));
			}
		}
	}

	@GlobalCallback(PlayerAllianceDisbandCallback.class)
	/** 解散 / disband. */
	public static void disband(PlayerAlliance alliance) {
		Preconditions.checkState(alliance.onlineMembers() <= 1,
				"Can't disband alliance with more than one online member");
		alliances.remove(alliance.getTeamId());
		alliance.onEvent(new AllianceDisbandEvent(alliance));
	}

	/** 更换队长 / change Leader. */
	public static void changeLeader(Player player) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		if (alliance != null) {
			alliance.onEvent(new ChangeAllianceLeaderEvent(alliance, player));
		}
	}

	/** 更换副队长 / change Vice Captain. */
	public static void changeViceCaptain(Player player, AssignType assignType) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		if (alliance != null) {
			alliance.onEvent(new AssignViceCaptainEvent(alliance, player, assignType));
		}
	}

	/** 搜索联盟。 / Search alliance. */
	public static final PlayerAlliance searchAlliance(Integer playerObjId) {
		for (PlayerAlliance alliance : alliances.values()) {
			if (alliance.hasMember(playerObjId)) {
				return alliance;
			}
		}
		return null;
	}

	/** 更换成员小队 / Change Member Group */
	public static void changeMemberGroup(Player player, int firstPlayer, int secondPlayer, int allianceGroupId) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		Preconditions.checkNotNull(alliance, "Alliance should not be null for group change");
		if (alliance.isLeader(player) || alliance.isViceCaptain(player)) {
			alliance.onEvent(new ChangeMemberGroupEvent(alliance, firstPlayer, secondPlayer, allianceGroupId));
		} else {
			PacketSendUtility.sendMessage(player, "You do not have the authority for that.");
		}
	}

	/**
	 * 检查就绪状态。 / Check readiness.
	 */
	public static void checkReady(Player player, TeamCommand eventCode) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		if (alliance != null) {
			alliance.onEvent(new CheckAllianceReadyEvent(alliance, player, eventCode));
		}
	}

	/** 分配基纳 / Distribute Kinah */
	public static void distributeKinah(Player player, long amount) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		if (alliance != null) {
			alliance.onEvent(new TeamKinahDistributionEvent<PlayerAlliance>(alliance, player, amount));
		}
	}

	/** 在队伍内分配基纳 / Distribute Kinah In Group */
	public static void distributeKinahInGroup(Player player, long amount) {
		PlayerAllianceGroup allianceGroup = player.getPlayerAllianceGroup2();
		if (allianceGroup != null) {
			allianceGroup.onEvent(new TeamKinahDistributionEvent<PlayerAllianceGroup>(allianceGroup, player, amount));
		}
	}

	/** 显示烙印标记 / show Brand. */
	public static void showBrand(Player player, int targetObjId, int brandId) {
		PlayerAlliance alliance = player.getPlayerAlliance2();
		if (alliance != null) {
			alliance.onEvent(new ShowBrandEvent<PlayerAlliance>(alliance, targetObjId, brandId));
		}
	}

	/** 获取服务状态。 / Returns the service status. */
	public static final String getServiceStatus() {
		return "Number of alliances: " + alliances.size();
	}

	public static class OfflinePlayerAllianceChecker implements Runnable, Predicate<PlayerAllianceMember> {
		private PlayerAlliance currentAlliance;

		/** 运行 / run. */
		@Override
		public void run() {
			for (PlayerAlliance alliance : alliances.values()) {
				currentAlliance = alliance;
				alliance.apply(this);
			}
			currentAlliance = null;
		}

		/** 应用。 / Apply. */
		@Override
		public boolean apply(PlayerAllianceMember member) {
			int kickDelay = currentAlliance.getTeamType().isAutoTeam() ? 60 : GroupConfig.ALLIANCE_REMOVE_TIME;
			if (!member.isOnline() && TimeUtil.isExpired(member.getLastOnlineTime() + kickDelay * 1000)) {
				if (currentAlliance.getTeamType().isOffence()) {
					GameLocationBootstrapServices.vortexService().removeInvaderPlayer(member.getObject());
				}
				currentAlliance.onEvent(
						new PlayerAllianceLeavedEvent(currentAlliance, member.getObject(), LeaveReson.LEAVE_TIMEOUT));
			}
			return true;
		}
	}
}
