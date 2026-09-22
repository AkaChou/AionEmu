package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.LegionConfig;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.lifecycle.GameHousingServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.model.team.legion.LegionPermissionsMask;
import com.aionemu.gameserver.model.team.legion.LegionWarehouse;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * 军团判权与入团申请域：创建/邀请/踢人/权限/仓库/入团申请的校验与流程实现。
 * Legion restriction domain: permission checks and the join-request flow for create, invite, kick,
 * rights, warehouse and join requests.
 *
 * <p>本类从 {@link LegionService} 拆出，作为其内部判权与入团申请实现；对外调用仍统一经
 * {@link LegionService} 门面，公开方法签名保持不变。
 * Split out of {@link LegionService} as its internal permission/join-request implementation.
 * External callers keep going through the {@link LegionService} facade, whose public signatures
 * are unchanged.</p>
 *
 * @author Simple
 */
@Slf4j
final class LegionRestrictions {

	/** 宿主服务，用于读取军团缓存与成员缓存 / hosting service, used to read legion and member caches. */
	private final LegionService service;

	/**
	 * 绑定宿主军团服务。
	 * Binds the hosting legion service.
	 *
	 * @param service 军团服务 / legion service
	 */
	LegionRestrictions(LegionService service) {
		this.service = service;
	}

	/** 申请流域实现，按需构造 / join-request flow implementation, built on demand. */
	private LegionJoinRequests joinRequestsImpl;

	/**
	 * 惰性获取申请流域实现。
	 * Lazily resolves the join-request flow implementation.
	 */
	private LegionJoinRequests joinRequests() {
		LegionJoinRequests current = joinRequestsImpl;
		if (current == null) {
			current = new LegionJoinRequests(service, this);
			joinRequestsImpl = current;
		}
		return current;
	}

	/**
	 * 静态徽章信息。
	 * Static Emblem information
	 */
	private static final int MIN_EMBLEM_ID = 0;
	private static final int MAX_EMBLEM_ID = 49;

	/**
	 * 检查创建军团的全部限制条件。
	 * This method checks all restrictions for legion creation
	 *
	 * @param activePlayer
	 * @param legionName
	 *
	 * @return 允许 / 成功则为 true / true if allow to create a legion
	 */
	boolean canCreateLegion(Player activePlayer, String legionName, Npc creatorNpc) {
		/* Some reasons why legions can' be created */
		if (!isValidName(legionName)) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CREATE_INVALID_GUILD_NAME);
			return false;
		} else if (!isNearLegionCreator(activePlayer, creatorNpc)) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CREATE_TOO_FAR_FROM_CREATOR_NPC);
			return false;
		} else if (!isFreeName(legionName)) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CREATE_SAME_GUILD_EXIST);
			return false;
		} else if (activePlayer.isLegionMember()) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CREATE_ALREADY_BELONGS_TO_GUILD);
			return false;
		} else if (activePlayer.getInventory().getKinah() < LegionConfig.LEGION_CREATE_REQUIRED_KINAH) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CREATE_NOT_ENOUGH_MONEY);
			return false;
		}
		return true;
	}

	/**
	 * 检查邀请玩家加入军团的全部限制条件。
	 * This method checks all restrictions for invite player to legion
	 *
	 * @param activePlayer
	 * @param targetPlayer
	 *
	 * @return 允许 / 成功则为 true / true if can invite player
	 */
	boolean canInvitePlayer(Player activePlayer, Player targetPlayer) {
		Legion legion = activePlayer.getLegion();
		if (activePlayer.getLifeStats().isAlreadyDead()) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CANT_INVITE_WHEN_DEAD);
			return false;
		}
		if (isSelf(activePlayer, targetPlayer.getObjectId())) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_INVITE_SELF);
			return false;
		} else if (targetPlayer.isLegionMember()) {
			if (legion.isMember(targetPlayer.getObjectId())) {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_HE_IS_MY_GUILD_MEMBER(targetPlayer.getName()));
			} else {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_HE_IS_OTHER_GUILD_MEMBER(targetPlayer.getName()));
			}
			return false;
		} else // 不同种族 / Not Same Race
			if (!activePlayer.getLegionMember().hasRights(LegionPermissionsMask.INVITE)) {
			// 无权邀请 / No rights to invite
			return false;
		} else return activePlayer.getRace() == targetPlayer.getRace() || LegionConfig.LEGION_INVITEOTHERFACTION;
	}

	/**
	 * 检查将玩家踢出军团的全部限制条件。
	 * This method checks all restrictions for kicking a player from a legion
	 *
	 * @param activePlayer
	 * @param charName
	 *
	 * @return 允许 / 成功则为 true / true if can kick player
	 */
	boolean canKickPlayer(Player activePlayer, String charName) {
		/**
	 * 从缓存获取 LegionMemberEx，离线则读库。
	 * Get LegionMemberEx from cache or database if offline
	 */
		LegionMemberEx legionMember = service.getLegionMemberEx(charName);
		if (legionMember == null) {
			log.error(I18n.get("log.10437023e015", charName));
			return false;
		}

		// STR_GUILD_BANISH_DONT_HAVE_RIGHT_TO_BANISH
		Legion legion = activePlayer.getLegion();

		if (isSelf(activePlayer, legionMember.getObjectId())) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_BANISH_CANT_BANISH_SELF);
			return false;
		} else if (legionMember.isBrigadeGeneral()) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_BANISH_CAN_BANISH_MASTER);
			return false;
		} else if (legionMember.getRank() == activePlayer.getLegionMember().getRank()) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_BANISH_DONT_HAVE_RIGHT_TO_BANISH);
			return false;
		} else if (!legion.isMember(legionMember.getObjectId())) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_BANISH_DONT_HAVE_RIGHT_TO_BANISH);
			return false;
		} else if (!activePlayer.getLegionMember().hasRights(LegionPermissionsMask.KICK)) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_BANISH_DONT_HAVE_RIGHT_TO_BANISH);
			return false;
		}
		return true;
	}

	/**
	 * 检查任命军团长的全部限制条件。
	 * This method checks all restrictions for appointing brigade general
	 *
	 * @param activePlayer
	 * @param targetPlayer
	 *
	 * @return 允许 / 成功则为 true / true if can appoint brigade general
	 */
	boolean canAppointBrigadeGeneral(Player activePlayer, Player targetPlayer) {
		Legion legion = activePlayer.getLegion();
		if (!isBrigadeGeneral(activePlayer)) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MEMBER_RANK_DONT_HAVE_RIGHT);
			return false;
		}
		// 不在同一军团 / not in same legion
		if (isSelf(activePlayer, targetPlayer.getObjectId())) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_ERROR_SELF);
			return false;
		} else return legion.isMember(targetPlayer.getObjectId());
	}

	/**
	 * 检查任命军阶的全部限制条件。
	 * This method checks all restrictions for appointing rank
	 *
	 * @param activePlayer
	 * @param targetObjId
	 *
	 * @return 允许 / 成功则为 true / true if can appoint rank
	 */
	boolean canAppointRank(Player activePlayer, int targetObjId) {
		Legion legion = activePlayer.getLegion();
		if (!isBrigadeGeneral(activePlayer)) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MEMBER_RANK_DONT_HAVE_RIGHT);
			return false;
		}
		// 不在同一军团 / not in same legion
		if (isSelf(activePlayer, targetObjId)) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_ERROR_SELF);
			return false;
		} else return legion.isMember(targetObjId);
	}

	/**
	 * 检查修改自我介绍的全部限制条件。
	 * This method checks all restrictions for changing self intro
	 *
	 * @param activePlayer
	 * @param newSelfIntro
	 *
	 * @return 允许 / 成功则为 true / true if allowed to change self intro
	 */
	boolean canChangeSelfIntro(Player activePlayer, String newSelfIntro) {
		return isValidSelfIntro(newSelfIntro);
	}

	/**
	 * 检查变更军团等级的全部限制条件。
	 * This method checks all restrictions for changing legion level
	 *
	 * @param activePlayer
	 *
	 * @param activePlayer
	 * @return 允许 / 成功则为 true / true if allowed to change legion level
	 */
	boolean canChangeLevel(Player activePlayer) {
		Legion legion = activePlayer.getLegion();
		int levelContributionPrice = legion.getContributionPrice();

		if (legion.getLegionLevel() == LegionService.MAX_LEGION_LEVEL) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_LEVEL_CANT_LEVEL_UP);
			return false;
		} else if (LegionConfig.ENABLE_GUILD_TASK_REQ && legion.getLegionLevel() >= 5) {
			if (!GameHousingServices.challengeTaskService().canRaiseLegionLevel(legion.getLegionId(),
					legion.getLegionLevel())) {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_LEVEL_UP_CHALLENGE_TASK(legion.getLegionLevel()));
				return false;
			}
		} else if (activePlayer.getInventory().getKinah() < legion.getKinahPrice()) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_LEVEL_NOT_ENOUGH_MONEY);
			return false;
		} else if (!legion.hasRequiredMembers()) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_LEVEL_NOT_ENOUGH_MEMBER);
			return false;
		} else if (legion.getContributionPoints() < levelContributionPrice) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_LEVEL_NOT_ENOUGH_POINT);
			return false;
		}
		return true;
	}

	/**
	 * 处理军团相关逻辑。
	 * This method will check all restrictions for changing nickname
	 *
	 * @param legion
	 * @return true if allowed to change nickname of target player
	 */
	boolean canChangeNickname(Legion legion, int targetObjectId, String newNickname) {
		// 不在同一军团 / not in same legion
		if (!isValidNickname(newNickname)) {
			// 无效昵称 / invalid nickname
			return false;
		} else return legion.isMember(targetObjectId);
	}

	/**
	 * 检查修改公告的全部限制条件。
	 * This method checks all restrictions for changing announcements
	 *
	 * @param legionMember
	 * @param announcement
	 *
	 * @return 允许 / 成功则为 true / true if can change announcement
	 */
	boolean canChangeAnnouncement(LegionMember legionMember, String announcement) {
		return legionMember.hasRights(LegionPermissionsMask.EDIT)
				&& (announcement.isEmpty() || isValidAnnouncement(announcement));
	}

	/**
	 * 检查解散军团的全部限制条件。
	 * This method checks all restrictions for disband legion
	 *
	 * @param activePlayer
	 * @param legion
	 *
	 * @return 允许 / 成功则为 true / true if can disband legion
	 */
	boolean canDisbandLegion(Player activePlayer, Legion legion) {
		if (legion == null) {
			return false;
		}
		if (!isBrigadeGeneral(activePlayer)) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_DISPERSE_ONLY_MASTER_CAN_DISPERSE);
			return false;
		} else if (legion.getLegionWarehouse().getWhUser() != 0) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_DISPERSE_CANT_DISPERSE_GUILD_WHILE_USING_WAREHOUSE);
			return false;
		} else if (legion.isDisbanding()) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_DISPERSE_ALREADY_REQUESTED);
			return false;
		} else if (legion.getLegionWarehouse().size() > 0) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_DISPERSE_CANT_DISPERSE_GUILD_STORE_ITEM_IN_WAREHOUSE);
			return false;
		}
		return true;
	}

	/**
	 * 检查离开军团的全部限制条件。
	 * This method checks all restrictions for leaving
	 *
	 * @param activePlayer
	 *
	 * @param activePlayer
	 * @return 允许 / 成功则为 true / true if allowed to leave
	 */
	boolean canLeave(Player activePlayer) {
		if (isBrigadeGeneral(activePlayer)) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_LEAVE_MASTER_CANT_LEAVE_BEFORE_CHANGE_MASTER);
			return false;
		}
		return true;
	}

	/**
	 * 是否允许修改入团设置（仅旅长）。
	 * Whether the player may change join settings (brigade general only).
	 *
	 * @param activePlayer 操作玩家 / Acting player
	 * @return 允许修改时为 {@code true} / {@code true} if allowed
	 */
	public boolean canChangeLegionJoinSetting(Player activePlayer) {
		return isBrigadeGeneral(activePlayer);
	}

	/**
	 * 检查重建军团的全部限制条件。
	 * This method checks all restrictions for recreate legion
	 *
	 * @param activePlayer
	 * @param legion
	 *
	 * @return 允许 / 成功则为 true / true if allowed to recreate legion
	 */
	boolean canRecreateLegion(Player activePlayer, Legion legion) {
		// 军团未在解散 / Legion is not disbanding
		if (!isBrigadeGeneral(activePlayer)) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_DISPERSE_ONLY_MASTER_CAN_DISPERSE);
			return false;
		} else return legion.isDisbanding();
	}

	/**
	 * 检查上传徽章信息的全部限制条件。
	 * This method checks all restrictions for upload emblem info
	 *
	 * @param activePlayer
	 *
	 * @param activePlayer
	 * @return 允许 / 成功则为 true / true if allowed to upload emblem info
	 */
	boolean canUploadEmblemInfo(Player activePlayer) {
		if (!isBrigadeGeneral(activePlayer)) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_EMBLEM_DONT_HAVE_RIGHT);
			return false;
		} else if (activePlayer.getLegion().getLegionLevel() < 3) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_EMBLEM_DONT_HAVE_RIGHT);
			return false;
		}
		return true;
	}

	/**
	 * 检查上传徽章的全部限制条件。
	 * This method checks all restrictions for uploading emblem
	 *
	 * @param activePlayer
	 *
	 * @param activePlayer
	 * @return 允许 / 成功则为 true / true if allowed to upload emblem
	 */
	boolean canUploadEmblem(Player activePlayer) {
		if (!isBrigadeGeneral(activePlayer)) {
			// 不是军团长 / Not legion leader
			return false;
		} else // 未上传徽章 / Not uploading emblem
			if (activePlayer.getLegion().getLegionLevel() < 3) {
			// 军团等级不够高 / Legion level isn't high enough
			return false;
		} else return activePlayer.getLegion().getLegionEmblem().isUploading();
	}

	/**
	 * 是否允许打开军团仓库（成员状态、解散中、配置与占用锁）。
	 * Whether the player may open the legion warehouse (membership, disband, config, lock).
	 *
	 * @param player 操作玩家 / Acting player
	 * @return 允许打开时为 {@code true} / {@code true} if allowed
	 */
	public boolean canOpenWarehouse(Player player) {
		if (!player.isLegionMember()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NO_GUILD_TO_DEPOSIT);
			return false;
		}
		Legion legion = player.getLegion();
		LegionWarehouse legWh = legion.getLegionWarehouse();
		int whUser = legWh.getWhUser();
		int playerId = player.getObjectId();
		if (legion.isDisbanding()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_CANT_USE_WHILE_DISPERSE);
			return false;
		} else if (!LegionConfig.LEGION_WAREHOUSE) {
			// 军团仓库未启用 / Legion Warehouse not enabled
			return false;
		} else if (whUser != playerId && legWh.getWhUser() != 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_IN_USE);
			return false;
		}
		legWh.setWhUser(player.getObjectId());
		return true;
	}

	/**
	 * 是否允许保存军团徽章（ID 范围、等级与基纳）。
	 * Whether the player may store a legion emblem (id range, level and kinah).
	 *
	 * @param activePlayer 操作玩家 / Acting player
	 * @param legionId 军团 ID / Legion ID
	 * @param emblemId 徽章模板 ID / Emblem template ID
	 * @return 允许保存时为 {@code true} / {@code true} if allowed
	 */
	public boolean canStoreLegionEmblem(Player activePlayer, int legionId, int emblemId) {
		Legion legion = activePlayer.getLegion();
		if (emblemId < MIN_EMBLEM_ID || emblemId > MAX_EMBLEM_ID) {
			// 非有效徽章 ID / Not a valid emblemId
			return false;
		} else if (legionId != legion.getLegionId()) {
			// 军团 ID 不相等 / legion id not equal
			return false;
		} else if (legion.getLegionLevel() < 2) {
			// 军团等级不够高 / legion level not high enough
			return false;
		} else if (activePlayer.getInventory().getKinah() < LegionConfig.LEGION_EMBLEM_REQUIRED_KINAH) {
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_KINA(LegionConfig.LEGION_EMBLEM_REQUIRED_KINAH));
			return false;
		}
		return true;
	}

	/**
	 * 检查玩家是否为军团长，否则返回提示消息。 / Check whether the player is the brigade general and return a message otherwise.
	 *
	 * @param player
	 * @return
	 */
	private boolean isBrigadeGeneral(Player player) {
		return player.getLegionMember().isBrigadeGeneral();
	}

	/**
	 * 检查是否目标为相同作为当前玩家。 / Checks if target is same as current player
	 *
	 * @param player
	 * @param targetObjId
	 * @return
	 */
	private boolean isSelf(Player player, int targetObjId) {
		return player.sameObjectId(targetObjId);
	}

	/**
	 * 检查是否名称为已经占用与否。 / Checks if name is already taken or not
	 *
	 * @param name character name
	 * @return true if is free, false in other case
	 */
	private boolean isFreeName(String name) {
		return !DAOManager.getDAO(LegionDAO.class).isNameUsed(name);
	}

	/**
	 * 检查是否自我介绍为有效 . 其应包含仅英文字母。 / Checks if a self intro is valid. It should contain only english letters
	 *
	 * @param name character name
	 * @return true if name is valid, false overwise
	 */
	private boolean isValidSelfIntro(String name) {
		return LegionConfig.SELF_INTRO_PATTERN.matcher(name).matches();
	}

	/**
	 * 检查是否昵称为有效 . 其应包含仅英文字母。 / Checks if a nickname is valid. It should contain only english letters
	 *
	 * @param name character name
	 * @return true if name is valid, false overwise
	 */
	private boolean isValidNickname(String name) {
		return LegionConfig.NICKNAME_PATTERN.matcher(name).matches();
	}

	/**
	 * 检查是否公告为有效 . 其应包含仅英文字母。 / Checks if a announcement is valid. It should contain only english letters
	 *
	 * @param name announcement
	 * @return true if name is valid, false overwise
	 */
	private boolean isValidAnnouncement(String name) {
		return LegionConfig.ANNOUNCEMENT_PATTERN.matcher(name.replaceAll("\\r\\n", "")).matches();
	}

	/**
	 * 记录军团仓库存取物品历史（存入/取出）。
	 * Records legion warehouse item deposit/withdraw history.
	 *
	 * @param player Acting player
	 * @param itemId Item template id
	 * @param count Count
	 * @param sourceStorage Source storage
	 * @param destStorage Destination storage
	 */
	void addWHItemHistory(Player player, int itemId, long count, IStorage sourceStorage, IStorage destStorage) {
		joinRequests().addWHItemHistory(player, itemId, count, sourceStorage, destStorage);
	}

	/**
	 * 处理军团搜索：type=0 全量缓存，type=1 按名称模糊匹配。
	 * Handles legion search: type 0 all cached, type 1 name contains filter.
	 *
	 * Requesting player
	 * @param type 搜索类型 / Search type
	 * @param legionName 名称关键字 / Name keyword
	 */
	void handleLegionSearch(Player player, int type, String legionName) {
		joinRequests().handleLegionSearch(player, type, legionName);
	}

	/**
	 * 设置军团入团说明（仅旅长），并同步客户端与数据库。
	 * Sets the legion join description (brigade general only) and syncs client/DB.
	 *
	 * @param player Acting player
	 * @param description Join description
	 */
	void setJoinDescription(Player player, String description) {
		joinRequests().setJoinDescription(player, description);
	}

	/**
	 * 设置军团入团类型（仅旅长），并同步客户端与数据库。
	 * Sets the legion join type (brigade general only) and syncs client/DB.
	 *
	 * @param player Acting player
	 * @param joinType Join type
	 */
	void setJoinType(Player player, int joinType) {
		joinRequests().setJoinType(player, joinType);
	}

	/**
	 * 设置入团最低等级（仅旅长），并同步客户端与数据库。
	 * Sets the minimum join level (brigade general only) and syncs client/DB.
	 *
	 * @param player Acting player
	 * @param minLevel Minimum level
	 */
	void setJoinMinLevel(Player player, int minLevel) {
		joinRequests().setJoinMinLevel(player, minLevel);
	}

	/**
	 * 向玩家发送当前入团申请对应的军团信息包。
	 * Sends the join-request legion info packet to the player.
	 *
	 * Target player
	 * @param legionId 军团 ID，<=0 表示清空 / Legion id, <=0 clears
	 */
	void sendLegionJoinRequestPacket(Player player, int legionId) {
		joinRequests().sendLegionJoinRequestPacket(player, legionId);
	}

	/**
	 * 玩家进世界时，按 CommonData 中的申请军团 ID 重发入团申请信息包。
	 * On enter-world, resends join-request info using the legion id stored in CommonData.
	 *
	 * @param player Target player
	 */
	void sendLegionJoinRequestPacketonEnterWorld(Player player) {
		joinRequests().sendLegionJoinRequestPacketonEnterWorld(player);
	}

	/**
	 * 处理玩家入团申请：申请入队、直接加入或拒绝招募。
	 * Handles a player join request: apply, direct join, or reject if not recruiting.
	 *
	 * @param player Applying player
	 * @param legionId Target legion id
	 * @param joinType Join type
	 * @param joinRequestMsg Application message
	 */
	void handleLegionJoinRequest(Player player, int legionId, int joinType, String joinRequestMsg) {
		joinRequests().handleLegionJoinRequest(player, legionId, joinType, joinRequestMsg);
	}

	/**
	 * 取消玩家对指定军团的入团申请，并通知旅长。
	 * Cancels the player join request for a legion and notifies the brigade general.
	 *
	 * @param player Applying player
	 * @param legionId Legion id
	 */
	void handleJoinRequestCancel(Player player, int legionId) {
		joinRequests().handleJoinRequestCancel(player, legionId);
	}

	/**
	 * 玩家侧处理入团申请结果（接受则入团，拒绝则清理申请）。
	 * Applies join-request answer on the player side (join on accept, clear on deny).
	 *
	 * @param player Applying player
	 */
	void handleJoinRequestGetAnswer(Player player) {
		joinRequests().handleJoinRequestGetAnswer(player);
	}

	/**
	 * 旅长批复入团申请：在线则即时处理，离线则写库状态。
	 * Brigade general answers a join request: handles online immediately or persists offline state.
	 *
	 * @param brigadeGeneral Brigade general player
	 * @param playerId Applicant object id
	 * @param accept Whether accepted
	 */
	void handleJoinRequestGiveAnswer(Player brigadeGeneral, int playerId, boolean accept) {
		joinRequests().handleJoinRequestGiveAnswer(brigadeGeneral, playerId, accept);
	}

	/**
	 * 校验军团名称是否合法（匹配配置的正则）。
	 * Checks whether a legion name is valid (matches the configured pattern).
	 *
	 * @param name 军团名称 / Legion name
	 * @return 合法返回 true，否则 false / True if valid, false otherwise
	 */
	static boolean isValidName(String name) {
		return LegionConfig.LEGION_NAME_PATTERN.matcher(name).matches();
	}

	static boolean isNearLegionCreator(Player player, Npc creatorNpc) {
		return creatorNpc != null && MathUtil.isInRange(player, creatorNpc,
				creatorNpc.getObjectTemplate().getTalkDistance() + 2);
	}
}
