package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionHistoryType;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.model.team.legion.LegionRank;
import com.aionemu.gameserver.model.team.legion.LegionWarehouse;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ICON_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_ADD_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_MEMBERLIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_EMBLEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_TITLE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.collections.ListSplitter;
import com.aionemu.gameserver.world.container.LegionContainer;
import com.aionemu.gameserver.world.container.LegionMemberContainer;

/**
 * 军团成员域：成员缓存与持久化、加入/踢出/离开、登录与下线同步。
 * Legion member domain: member cache and persistence, join/kick/leave plus login/logout sync.
 *
 * <p>本类从 {@link LegionService} 拆出；军团级数据（军团缓存、历史、公告）仍通过宿主服务访问，
 * 对外调用仍统一经 {@link LegionService} 门面，公开方法签名保持不变。
 * Split out of {@link LegionService}; legion-level data (caches, history, announcements) is still
 * reached through the hosting service, and external callers keep using the {@link LegionService}
 * facade whose public signatures are unchanged.</p>
 */
@Slf4j
final class LegionMembers {

	/** 宿主服务，用于军团缓存、历史与公告。 / hosting service for caches, history and announcements. */
	private final LegionService service;

	/**
	 * 绑定宿主军团服务。
	 * Binds the hosting legion service.
	 *
	 * @param service 军团服务 / legion service
	 */
	LegionMembers(LegionService service) {
		this.service = service;
	}

	/** 军团成员缓存（宿主提供）。 / legion member cache, provided by the hosting service. */
	private LegionMemberContainer allCachedLegionMembers() {
		return service.allCachedLegionMembers();
	}

	/** 军团缓存（宿主提供）。 / legion cache, provided by the hosting service. */
	private LegionContainer allCachedLegions() {
		return service.allCachedLegions();
	}

	/**
	 * 将军团成员数据存入数据库，或保存新成员。
	 * Stores legion member data into db or saves a new one
	 *
	 * legion member
	 * @param newMember 是否新成员 / new member
	 */
	void storeLegionMember(LegionMember legionMember, boolean newMember) {
		if (newMember) {
			addCachedLegionMember(legionMember);
			DAOManager.getDAO(LegionMemberDAO.class).saveNewLegionMember(legionMember);
		} else {
			DAOManager.getDAO(LegionMemberDAO.class).storeLegionMember(legionMember.getObjectId(), legionMember);
		}
	}

	/**
	 * 存储军团成员。
	 * Stores a legion member
	 *
	 * @param legionMember legion member
	 */
	void storeLegionMember(LegionMember legionMember) {
		storeLegionMember(legionMember, false);
	}

	/**
	 * 将军团成员数据存入数据库。
	 * Stores legion member data into database
	 *
	 * @param player 玩家 / player
	 */
	void storeLegionMemberExInCache(Player player) {
		if (this.allCachedLegionMembers().containsEx(player.getObjectId())) {
			LegionMemberEx legionMemberEx = allCachedLegionMembers().getMemberEx(player.getObjectId());
			legionMemberEx.setNickname(player.getLegionMember().getNickname());
			legionMemberEx.setSelfIntro(player.getLegionMember().getSelfIntro());
			legionMemberEx.setPlayerClass(player.getPlayerClass());
			legionMemberEx.setExp(player.getCommonData().getExp());
			legionMemberEx.setLastOnline(player.getCommonData().getLastOnline());
			legionMemberEx.setWorldId(player.getPosition().getMapId());
			legionMemberEx.setOnline(false);
		} else {
			LegionMemberEx legionMemberEx = new LegionMemberEx(player, player.getLegionMember(), false);
			addCachedLegionMemberEx(legionMemberEx);
		}
	}

	/**
	 * 将新军团成员加入缓存。
	 * This method will add a new legion member to the cache
	 *
	 * @param legionMember legion member
	 */
	void addCachedLegionMember(LegionMember legionMember) {
		this.allCachedLegionMembers().addMember(legionMember);
	}

	/**
	 * 将新军团成员加入缓存。
	 * This method will add a new legion member to the cache
	 *
	 * @param legionMemberEx 扩展军团成员 / legion member ex
	 */
	void addCachedLegionMemberEx(LegionMemberEx legionMemberEx) {
		this.allCachedLegionMembers().addMemberEx(legionMemberEx);
	}

	/**
	 * 从缓存与数据库移除军团成员。
	 * This method will remove the legion member from cache and the database
	 *
	 * @param legionMember legion member
	 */
	void deleteLegionMemberFromDB(LegionMemberEx legionMember) {
		this.allCachedLegionMembers().remove(legionMember);
		DAOManager.getDAO(LegionMemberDAO.class).deleteLegionMember(legionMember.getObjectId());
		Legion legion = legionMember.getLegion();
		legion.deleteLegionMember(legionMember.getObjectId());
		service.addHistory(legion, legionMember.getName(), LegionHistoryType.KICK);
	}

	/**
	 * 返回离线军团成员给定 playerId (若该成员存在)。 / Returns the offline legion member with given playerId (if such member exists)
	 *
	 * @param playerObjId
	 * @return LegionMemberEx
	 */
	LegionMemberEx getLegionMemberEx(int playerObjId) {
		if (this.allCachedLegionMembers().containsEx(playerObjId)) {
			return this.allCachedLegionMembers().getMemberEx(playerObjId);
		} else {
			LegionMemberEx legionMember = DAOManager.getDAO(LegionMemberDAO.class).loadLegionMemberEx(playerObjId);
			addCachedLegionMemberEx(legionMember);
			return legionMember;
		}
	}

	/**
	 * 返回离线军团成员给定 playerId (若该成员存在)。 / Returns the offline legion member with given playerId (if such member exists)
	 *
	 * @param playerName
	 * @return LegionMemberEx
	 */
	LegionMemberEx getLegionMemberEx(String playerName) {
		if (this.allCachedLegionMembers().containsEx(playerName)) {
			return this.allCachedLegionMembers().getMemberEx(playerName);
		} else {
			LegionMemberEx legionMember = DAOManager.getDAO(LegionMemberDAO.class).loadLegionMemberEx(playerName);
			addCachedLegionMemberEx(legionMember);
			return legionMember;
		}
	}

	/**
	 * 加载军团成员扩展列表（在线优先构造，离线从缓存/DB），可排除指定 objectId。
	 * Loads extended legion member list (online first, offline from cache/DB); optional objectId exclusion.
	 *
	 * Target legion
	 *
	 * @param objExcluded 需排除的玩家 objectId，可为 null / Object id to exclude, or null
	 * @param objExcluded
	 * @return 成员扩展列表 / Extended member list
	 */
	public ArrayList<LegionMemberEx> loadLegionMemberExList(Legion legion, Integer objExcluded) {
		ArrayList<LegionMemberEx> legionMembers = new ArrayList<>();
		for (Integer memberObjId : legion.getLegionMembers()) {
			LegionMemberEx legionMemberEx;
			if (objExcluded != null && objExcluded.equals(memberObjId)) {
				continue;
			}
			Player memberPlayer = service.world().findPlayer(memberObjId);
			if (memberPlayer != null) {
				legionMemberEx = new LegionMemberEx(memberPlayer, memberPlayer.getLegionMember(), true);
			} else {
				legionMemberEx = getLegionMemberEx(memberObjId);
			}
			legionMembers.add(legionMemberEx);
		}
		return legionMembers;
	}

	/**
	 * 以志愿兵军阶将新成员加入军团。
	 * This method will add a new legion member to a legion with VOLUNTEER rank
	 *
	 * @param legion legion
	 * @param player 玩家 / player
	 */
	void addLegionMember(Legion legion, Player player) {
		addLegionMember(legion, player, LegionRank.VOLUNTEER);
	}

	/**
	 * 以指定军阶将新成员加入军团。
	 * This method will add a new legion member to a legion with input rank
	 *
	 * @param legion legion
	 * @param player 玩家 / player
	 * @param rank rank
	 */
	void addLegionMember(Legion legion, Player player, LegionRank rank) {
		player.setLegionMember(new LegionMember(player.getObjectId(), legion, rank));
		storeLegionMember(player.getLegionMember(), true);
		PacketSendUtility.sendPacket(player, new SM_LEGION_INFO(legion));
		ArrayList<LegionMemberEx> totalMembers = loadLegionMemberExList(legion, player.getObjectId());
		ListSplitter<LegionMemberEx> splits = new ListSplitter<>(totalMembers, 128);
		boolean isFirst = true;
		while (!splits.isLast()) {
			boolean result = false;
			List<LegionMemberEx> curentMembers = splits.getNext();
			if (isFirst && curentMembers.size() < totalMembers.size()) {
				result = true;
			}
			PacketSendUtility.sendPacket(player, new SM_LEGION_MEMBERLIST(curentMembers, result, isFirst));
			isFirst = false;
		}
		PacketSendUtility.broadcastPacketToLegion(legion,
				new SM_LEGION_ADD_MEMBER(player, false, 1300260, player.getName()), player.getObjectId());
		PacketSendUtility.sendPacket(player, new SM_LEGION_ADD_MEMBER(player, false, 0, ""));
		LegionEmblem legionEmblem = legion.getLegionEmblem();
		PacketSendUtility.broadcastPacket(player,
				new SM_LEGION_UPDATE_EMBLEM(legion.getLegionId(), legionEmblem.getEmblemId(), legionEmblem.getColor_r(),
						legionEmblem.getColor_g(), legionEmblem.getColor_b(), legionEmblem.getEmblemType()),
				true);
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x08));
		PacketSendUtility.broadcastPacket(player, new SM_LEGION_UPDATE_TITLE(player.getObjectId(), legion.getLegionId(),
				legion.getLegionName(), player.getLegionMember().getRank().getRankId()), true);
		legion.addBonus();
	}

	/**
	 * 移除军团成员。
	 * This method will remove a legion member
	 *
	 * @param charName 角色名称 / Character name
	 * @param kick 是否由其他成员踢出 / Whether another member is kicking the character
	 * @param playerName 操作者名称 / Acting player name
	 * @return 移除成功时为 {@code true} / {@code true} if removed successfully
	 */
	boolean removeLegionMember(String charName, boolean kick, String playerName) {
		/**
	 * 从缓存获取 LegionMemberEx，离线则读库。
	 * Get LegionMemberEx from cache or database if offline
	 */
		LegionMemberEx legionMember = service.getLegionMemberEx(charName);
		if (legionMember == null) {
			log.error(I18n.get("log.10437023e015", charName));
			return false;
		}

		/**
	 * 从数据库和缓存中删除军团成员。 / Delete the legion member from the database and cache.
	 */
		deleteLegionMemberFromDB(legionMember);

		/**
	 * 若玩家在线则发包并重置军团成员信息。
	 * If player is online send packet and reset legion member
	 */
		Player player = service.world().findPlayer(charName);
		if (player != null) {
			PacketSendUtility.broadcastPacket(player, new SM_LEGION_UPDATE_TITLE(player.getObjectId(), 0, "", 2), true);
		}
		Legion legion = legionMember.getLegion();
		/**
	 * 发送数据包到军团成员。 / Send packets to legion members
	 */
		if (kick) {
			PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_LEAVE_MEMBER(1300247,
					legionMember.getObjectId(), playerName, legionMember.getName()));
		} else {
			PacketSendUtility.broadcastPacketToLegion(legion,
					new SM_LEGION_LEAVE_MEMBER(900699, legionMember.getObjectId(), charName));
		}
		legion.removeBonus();
		return true;
	}

	/**
	 * 将玩家自身移出军团（不经 kick 流程），并清理加成图标。
	 * Removes the player from their legion as a voluntary leave and clears bonus icons.
	 *
	 * @param player 目标玩家 / Target player
	 * @return 移除成功时为 {@code true} / {@code true} if removed
	 */
	public boolean removePlayerFromLegionAsItself(Player player) {
		if (removeLegionMember(player.getName(), false, "")) {
			Legion legion = player.getLegion();
			PacketSendUtility.sendPacket(player, new SM_LEGION_LEAVE_MEMBER(1300241, 0, legion.getLegionName()));
			player.resetLegionMember();
			if (legion.hasBonus()) {
				PacketSendUtility.sendPacket(player, new SM_ICON_INFO(1, false));
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 玩家登录时同步军团信息：成员列表、公告、解散状态与加成。
	 * On login, syncs legion info: member list, announcement, disband state and bonuses.
	 *
	 * @param activePlayer Logging-in player
	 */
	public void onLogin(Player activePlayer) {
		Legion legion = activePlayer.getLegion();
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_UPDATE_MEMBER(activePlayer, 0, ""),
				activePlayer.getObjectId());
		PacketSendUtility.broadcastPacketToLegion(legion,
				SM_SYSTEM_MESSAGE.STR_MSG_NOTIFY_LOGIN_GUILD(activePlayer.getName()), activePlayer.getObjectId());
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_ADD_MEMBER(activePlayer, true, 0, ""));
		PacketSendUtility.sendPacket(activePlayer, new SM_LEGION_INFO(legion));
		ArrayList<LegionMemberEx> totalMembers = loadLegionMemberExList(legion, null);
		ListSplitter<LegionMemberEx> splits = new ListSplitter<>(totalMembers, 128);
		boolean isFirst = true;
		while (!splits.isLast()) {
			boolean result = false;
			List<LegionMemberEx> curentMembers = splits.getNext();
			if (isFirst && curentMembers.size() < totalMembers.size()) {
				result = true;
			}
			PacketSendUtility.sendPacket(activePlayer, new SM_LEGION_MEMBERLIST(curentMembers, result, isFirst));
			isFirst = false;
		}
		service.displayLegionMessage(activePlayer, legion.getCurrentAnnouncement());
		if (legion.isDisbanding())
			PacketSendUtility.sendPacket(activePlayer, new SM_LEGION_EDIT(0x06, legion.getDisbandTime()));
		if (legion.hasBonus()) {
			PacketSendUtility.sendPacket(activePlayer, new SM_ICON_INFO(1, true));
		} else {
			legion.addBonus();
		}
	}

	/**
	 * 玩家下线时释放仓库占用、广播离线并持久化军团/成员数据。
	 * On logout, releases warehouse lock, broadcasts offline status and persists legion/member data.
	 *
	 * @param player Logging-out player
	 */
	public void onLogout(Player player) {
		Legion legion = player.getLegion();
		LegionWarehouse lwh = player.getLegion().getLegionWarehouse();
		if (lwh.getWhUser() == player.getObjectId()) {
			lwh.setWhUser(0);
		}
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_UPDATE_MEMBER(player));
		service.storeLegion(legion);
		storeLegionMember(player.getLegionMember());
		storeLegionMemberExInCache(player);
		service.storeLegionAnnouncements(legion);
		legion.removeBonus();
	}
}
