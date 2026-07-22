package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.LegionConfig;
import com.aionemu.gameserver.dao.*;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.lifecycle.GameHousingServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.DeniedStatus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.*;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.collections.ListSplitter;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.container.LegionContainer;
import com.aionemu.gameserver.world.container.LegionMemberContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 军团服务，负责军团的加载/持久化、成员管理、仓库、徽章与权限。
 * Legion service responsible for loading/storing legions, members, warehouse, emblem and permissions.
 *
 * @author Simple modified by cura, Source
 */
@Slf4j
public class LegionService {

	private static volatile ObjectProvider<LegionService> instanceProvider;
	private final LegionContainer allCachedLegions = new LegionContainer();
	private final LegionMemberContainer allCachedLegionMembers = new LegionMemberContainer();
	private World world;
	/** 踢出成员的军团动作操作码 / Legion action opcode for kicking a member. */
	public final static int LEGION_ACTION_KICK = 4;
	/** 军团最高等级 / Maximum legion level. */
	private static final int MAX_LEGION_LEVEL = 8;
	/** 军团排行缓存 / Legion ranking cache. */
	private Map<Integer, Integer> legionRanking;
	/** 军团操作限制校验器 / Legion operation restriction checker. */
	private LegionRestrictions legionRestrictions = new LegionRestrictions();

	/**
	 * 获取军团服务单例（优先 Spring ObjectProvider，否则回退内部 holder）。
	 * Returns the legion service singleton (Spring ObjectProvider first, else internal holder).
	 *
	 * @return 军团服务实例 / Legion service instance
	 */
	public static LegionService getInstance() {
		ObjectProvider<LegionService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 的 ObjectProvider，用于容器托管的实例解析。
	 * Injects the Spring ObjectProvider used for container-managed instance resolution.
	 *
	 * @param provider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<LegionService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 构造军团服务并绑定世界引用。
	 * Constructs the legion service and binds the world reference.
	 */
	public LegionService() {
		this.world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
	}

	/**
	 * 校验军团名称是否合法（匹配配置的正则）。
	 * Checks whether a legion name is valid (matches the configured pattern).
	 *
	 * @param name 军团名称 / Legion name
	 * @return 合法返回 true，否则 false / True if valid, false otherwise
	 */
	public boolean isValidName(String name) {
		return LegionConfig.LEGION_NAME_PATTERN.matcher(name).matches();
	}

	/**
	 * 将军团数据存入数据库。
	 * Stores legion data into db
	 *
	 * legion
	 * @param newLegion 是否新军团 / new legion
	 */
	private void storeLegion(Legion legion, boolean newLegion) {
		if (newLegion) {
			addCachedLegion(legion);
			DAOManager.getDAO(LegionDAO.class).saveNewLegion(legion);
		} else {
			DAOManager.getDAO(LegionDAO.class).storeLegion(legion);
			DAOManager.getDAO(LegionDAO.class).storeLegionEmblem(legion.getLegionId(), legion.getLegionEmblem());
		}
	}

	/**
	 * 存储新创建的军团。
	 * Stores newly created legion
	 *
	 * legion
	 */
	private void storeLegion(Legion legion) {
		storeLegion(legion, false);
	}

	/**
	 * 将军团成员数据存入数据库，或保存新成员。
	 * Stores legion member data into db or saves a new one
	 *
	 * legion member
	 * @param newMember 是否新成员 / new member
	 */
	private void storeLegionMember(LegionMember legionMember, boolean newMember) {
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
	 * legion member
	 */
	private void storeLegionMember(LegionMember legionMember) {
		storeLegionMember(legionMember, false);
	}

	/**
	 * 将军团成员数据存入数据库。
	 * Stores legion member data into database
	 *
	 * @param player 玩家 / player
	 */
	private void storeLegionMemberExInCache(Player player) {
		if (this.allCachedLegionMembers.containsEx(player.getObjectId())) {
			LegionMemberEx legionMemberEx = allCachedLegionMembers.getMemberEx(player.getObjectId());
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
	 * 仅当军团在缓存中时获取。
	 * Gets a legion ONLY if he is in the cache
	 *
	 * @param legionId
	 * @return 军团，未缓存则为 null / Legion or null if not cached
	 */
	private Legion getCachedLegion(int legionId) {
		return this.allCachedLegions.get(legionId);
	}

	/**
	 * 仅当军团在缓存中时获取。
	 * Gets a legion ONLY if he is in the cache
	 *
	 * @param legionName
	 * @return 军团，未缓存则为 null / Legion or null if not cached
	 */
	private Legion getCachedLegion(String legionName) {
		return this.allCachedLegions.get(legionName);
	}

	/**
	 * 返回已缓存军团的迭代器。
	 * Returns an iterator over cached legions.
	 *
	 * @return 缓存军团迭代器 / Cached legion iterator
	 */
	public Iterator<Legion> getCachedLegionIterator() {
		return allCachedLegions.iterator();
	}

	/**
	 * 将新军团加入缓存。
	 * This method will add a new legion to the cache
	 *
	 * legion
	 */
	private void addCachedLegion(Legion legion) {
		this.allCachedLegions.add(legion);
	}

	/**
	 * 将新军团成员加入缓存。
	 * This method will add a new legion member to the cache
	 *
	 * legion member
	 */
	private void addCachedLegionMember(LegionMember legionMember) {
		this.allCachedLegionMembers.addMember(legionMember);
	}

	/**
	 * 将新军团成员加入缓存。
	 * This method will add a new legion member to the cache
	 *
	 * @param legionMemberEx 扩展军团成员 / legion member ex
	 */
	private void addCachedLegionMemberEx(LegionMemberEx legionMemberEx) {
		this.allCachedLegionMembers.addMemberEx(legionMemberEx);
	}

	/**
	 * 从数据库与缓存彻底移除军团。
	 * Completely removes legion from database and cache
	 *
	 * legion
	 */
	private void deleteLegionFromDB(Legion legion) {
		this.allCachedLegions.remove(legion);
		DAOManager.getDAO(LegionDAO.class).deleteLegion(legion.getLegionId());
	}

	/**
	 * 从缓存与数据库移除军团成员。
	 * This method will remove the legion member from cache and the database
	 *
	 * legion member
	 */
	private void deleteLegionMemberFromDB(LegionMemberEx legionMember) {
		this.allCachedLegionMembers.remove(legionMember);
		DAOManager.getDAO(LegionMemberDAO.class).deleteLegionMember(legionMember.getObjectId());
		Legion legion = legionMember.getLegion();
		legion.deleteLegionMember(legionMember.getObjectId());
		addHistory(legion, legionMember.getName(), LegionHistoryType.KICK);
	}

	/**
	 * 按名称获取军团（先查缓存，未命中则从数据库加载并缓存）。
	 * Returns the legion by name (cache first, then load from DB and cache).
	 *
	 * Legion name
	 *
	 * @param legionName
	 * @return 军团实例；不存在时可能为 null / Legion instance, or null if missing
	 */
	public Legion getLegion(String legionName) {
		/**
	 * 先检查军团是否已在缓存中。
	 * First check if our legion already exists in our Cache
	 */
		if (allCachedLegions.contains(legionName)) {
			Legion legion = getCachedLegion(legionName);
			return legion;
		}

		/**
	 * 否则从数据库加载军团信息。
	 * Else load the legion information from the database
	 */
		Legion legion = DAOManager.getDAO(LegionDAO.class).loadLegion(legionName);

		/**
		 * 处理其余需加载的信息。
	 * This will handle the rest of the information that needs to be loaded
		 */
		loadLegionInfo(legion);

		/**
	 * 将军团加入缓存。 / Add the legion to the cache.
	 */
		addCachedLegion(legion);

		/**
	 * 返回军团。 / Return the legion
	 */
		return legion;
	}

	/**
	 * 按 ID 获取军团（先查缓存，未命中则从数据库加载并缓存）。
	 * Returns the legion by id (cache first, then load from DB and cache).
	 *
	 * Legion id
	 * Legion instance
	 */
	public Legion getLegion(int legionId) {
		/**
	 * 先检查军团是否已在缓存中。
	 * First check if our legion already exists in our Cache
	 */
		if (allCachedLegions.contains(legionId)) {
			Legion legion = getCachedLegion(legionId);
			return legion;
		}

		/**
	 * 否则从数据库加载军团信息。
	 * Else load the legion information from the database
	 */
		Legion legion = DAOManager.getDAO(LegionDAO.class).loadLegion(legionId);

		/**
		 * 处理其余需加载的信息。
	 * This will handle the rest of the information that needs to be loaded
		 */
		loadLegionInfo(legion);

		/**
	 * 将军团加入缓存。 / Add the legion to the cache.
	 */
		addCachedLegion(legion);

		/**
	 * 返回军团。 / Return the legion
	 */
		return legion;
	}

	/**
	 * 加载军团信息。
	 * This method will load the legion information
	 *
	 * legion
	 */
	private void loadLegionInfo(Legion legion) {
		/**
	 * 检查是否军团为非空。 / Check if legion is not null
	 */
		if (legion == null) {
			return;
		}
		/**
	 * 加载并添加军团成员到军团。 / Load and add the legion members to legion
	 */
		legion.setLegionMembers(DAOManager.getDAO(LegionMemberDAO.class).loadLegionMembers(legion.getLegionId()));

		/**
	 * 加载并设置公告列表。 / Load and set the announcement list
	 */
		legion.setAnnouncementList(DAOManager.getDAO(LegionDAO.class).loadAnnouncementList(legion.getLegionId()));

		/**
		 * 设置军团徽章。
	 * Set legion emblem
		 */
		legion.setLegionEmblem(DAOManager.getDAO(LegionDAO.class).loadLegionEmblem(legion.getLegionId()));

		/**
	 * 加载军团仓库。 / Load Legion Warehouse
	 */
		legion.setLegionWarehouse(DAOManager.getDAO(LegionDAO.class).loadLegionStorage(legion));

		if (legionRanking.containsKey(legion.getLegionId())) {
			legion.setLegionRank(legionRanking.get(legion.getLegionId()));
		}
		/**
	 * 加载军团历史。 / Load Legion History
	 */
		DAOManager.getDAO(LegionDAO.class).loadLegionHistory(legion);
	}

	/**
	 * 返回指定军团团长（旅长）的玩家 objectId。
	 * Returns the object id of the brigade general for the given legion.
	 *
	 * Legion id
	 *
	 * @param legionId
	 * @return 团长 objectId；未找到时为 0 / Brigade general objectId, or 0 if not found
	 */
	public int getLegionBGeneral(int legionId) {
		Legion legion = getLegion(legionId);
		int legionBG = 0;

		for (int memberObjId : legion.getLegionMembers()) {
			LegionMember legionMember = getLegionMember(memberObjId);
			if (legionMember.getRank() == LegionRank.BRIGADE_GENERAL) {
				legionBG = memberObjId;
			}
		}
		return legionBG;
	}

	/**
	 * 按玩家 objectId 获取军团成员（缓存/数据库），若军团已到期解散则返回 null。
	 * Returns the legion member by player object id (cache/DB); null if the legion has finished disbanding.
	 *
	 * Player object id
	 *
	 * @param playerObjId
	 * @return 军团成员，或 null / Legion member, or null
	 */
	public LegionMember getLegionMember(int playerObjId) {
		LegionMember legionMember = null;
		if (this.allCachedLegionMembers.contains(playerObjId)) {
			legionMember = this.allCachedLegionMembers.getMember(playerObjId);
		} else {
			legionMember = DAOManager.getDAO(LegionMemberDAO.class).loadLegionMember(playerObjId);
			if (legionMember != null) {
				addCachedLegionMember(legionMember);
			}
		}

		if (legionMember != null) {
			if (checkDisband(legionMember.getLegion())) {
				return null;
			}
		}
		return legionMember;
	}

	/**
	 * 检查军团是否处于解散中。
	 * Method that checks if a legion is disbanding
	 *
	 * legion
	 *
	 * @param legion 若 it's time to be deleted 则为 true / true if it's time to be deleted
	 */
	private boolean checkDisband(Legion legion) {
		if (legion.isDisbanding()) {
			if ((System.currentTimeMillis() / 1000) > legion.getDisbandTime()) {
				disbandLegion(legion);
				return true;
			}
		}
		return false;
	}

	/**
	 * 立即解散军团：清理成员缓存、要塞关联，并更新在线成员后删除数据。
	 * Immediately disbands a legion: clears member cache, siege links, updates online members and deletes data.
	 *
	 * Target legion
	 */
	public void disbandLegion(Legion legion) {
		for (Integer memberObjId : legion.getLegionMembers()) {
			this.allCachedLegionMembers.remove(getLegionMemberEx(memberObjId));
		}
		GameFeatureServices.siegeService().cleanLegionId(legion.getLegionId());
		updateAfterDisbandLegion(legion);
		deleteLegionFromDB(legion);
	}

	/**
	 * 返回离线军团成员给定 playerId (若该成员存在)。 / Returns the offline legion member with given playerId (if such member exists)
	 *
	 * @param playerObjId
	 * @return LegionMemberEx
	 */
	private LegionMemberEx getLegionMemberEx(int playerObjId) {
		if (this.allCachedLegionMembers.containsEx(playerObjId)) {
			return this.allCachedLegionMembers.getMemberEx(playerObjId);
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
	private LegionMemberEx getLegionMemberEx(String playerName) {
		if (this.allCachedLegionMembers.containsEx(playerName)) {
			return this.allCachedLegionMembers.getMemberEx(playerName);
		} else {
			LegionMemberEx legionMember = DAOManager.getDAO(LegionMemberDAO.class).loadLegionMemberEx(playerName);
			addCachedLegionMemberEx(legionMember);
			return legionMember;
		}
	}

	/**
	 * 处理军团解散申请：校验权限后弹出确认框，接受则设置解散倒计时。
	 * Handles a legion disband request: validates rights, shows confirm dialog, and schedules disband on accept.
	 *
	 * Triggering NPC
	 * Requesting player
	 */
	public void requestDisbandLegion(Creature npc, final Player activePlayer) {
		final Legion legion = activePlayer.getLegion();
		if (legionRestrictions.canDisbandLegion(activePlayer, legion)) {
			RequestResponseHandler disbandResponseHandler = new RequestResponseHandler(npc) {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					int unixTime = (int) ((System.currentTimeMillis() / 1000) + LegionConfig.LEGION_DISBAND_TIME);
					legion.setDisbandTime(unixTime);
					updateMembersOfDisbandLegion(legion, unixTime);
				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
					// 无消息 / no message
				}
			};

			boolean disbandResult = activePlayer.getResponseRequester()
					.putRequest(SM_QUESTION_WINDOW.STR_GUILD_DISPERSE_STAYMODE, disbandResponseHandler);
			if (disbandResult) {
				PacketSendUtility.sendPacket(activePlayer,
						new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_GUILD_DISPERSE_STAYMODE, 0, 0));
			}
		}
	}

	/**
	 * 创建军团：扣费、写入数据库，并将创建者设为旅长。
	 * Creates a legion: charges kinah, persists data, and sets the creator as brigade general.
	 *
	 * Creator player
	 * Legion name
	 * legion creator NPC
	 */
	public void createLegion(Player activePlayer, String legionName, Npc creatorNpc) {
		if (legionRestrictions.canCreateLegion(activePlayer, legionName, creatorNpc)) {
			/**
	 * 创建新军团并放入发起者作为首位成员。 / Create new legion and put originator as first member
	 */
			Legion legion = new Legion(GameWorldBootstrapServices.idFactory().nextId(), legionName);
			legion.addLegionMember(activePlayer.getObjectId());

			activePlayer.getInventory().decreaseKinah(LegionConfig.LEGION_CREATE_REQUIRED_KINAH);

			/**
	 * 创建 LegionMember ,添加其到军团并绑定其到玩家。 / Create a LegionMember, add it to the legion and bind it to a Player
	 */
			storeLegion(legion, true);
			Timestamp currentTime = new Timestamp(System.currentTimeMillis());
			storeNewAnnouncement(legion.getLegionId(), currentTime, "");
			legion.addAnnouncementToList(currentTime, "");
			addLegionMember(legion, activePlayer, LegionRank.BRIGADE_GENERAL);
			PacketSendUtility.broadcastPacketToLegion(legion,
					new SM_LEGION_EDIT(0x05, (int) (System.currentTimeMillis() / 1000), ""));
			/**
	 * 添加并保存军团创建与加入历史。 / Add and save legion creation and join history.
	 */
			addHistory(legion, "", LegionHistoryType.CREATE);
			addHistory(legion, activePlayer.getName(), LegionHistoryType.JOIN);

			/**
	 * 发送所需数据包。 / Send required packets
	 */
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CREATED(legion.getLegionName()));
		}
	}

	static boolean isNearLegionCreator(Player player, Npc creatorNpc) {
		return creatorNpc != null && MathUtil.isInRange(player, creatorNpc,
				creatorNpc.getObjectTemplate().getTalkDistance() + 2);
	}

	/**
	 * 按军团 ID 将玩家直接加入军团（不走邀请流程）。
	 * Directly adds a player to the legion by id (bypasses invite flow).
	 *
	 * Legion id
	 * Target player
	 *
	 * @return 若 joined successfully 则为 true / True if joined successfully
	 */
	public boolean directAddPlayer(int legionId, Player player) {
		Legion legion = getLegion(legionId);
		if (legion == null) {
			return false;
		}
		return directAddPlayer(legion, player);
	}

	/**
	 * 将玩家直接加入指定军团（不走邀请流程），并广播加入历史。
	 * Directly adds a player to the given legion (bypasses invite) and records join history.
	 *
	 * Target legion
	 * Target player
	 *
	 * @return 若 joined successfully 则为 true / True if joined successfully
	 */
	public boolean directAddPlayer(Legion legion, Player player) {
		int playerObjId = player.getObjectId();
		if (legion.addLegionMember(playerObjId)) {
			// 将军团成员绑定到玩家 / Bind LegionMember to Player
			addLegionMember(legion, player);

			// 显示当前公告 / Display current announcement
			displayLegionMessage(player, legion.getCurrentAnnouncement());

			// 加入军团历史 / Add to history of legion
			addHistory(legion, player.getName(), LegionHistoryType.JOIN);
			return true;
		} else {
			player.resetLegionMember();
			return false;
		}
	}

	/**
	 * 处理军团邀请。
	 * Method that will handle a invitation to a legion
	 *
	 * active player
	 * target player
	 */
	private void invitePlayerToLegion(final Player activePlayer, final Player targetPlayer) {
		if (legionRestrictions.canInvitePlayer(activePlayer, targetPlayer)) {
			final Legion legion = activePlayer.getLegion();
			RequestResponseHandler responseHandler = new RequestResponseHandler(activePlayer) {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (!targetPlayer.getCommonData().isOnline()) {
						PacketSendUtility.sendPacket(activePlayer,
								SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(targetPlayer.getName()));
					} else {
						int playerObjId = targetPlayer.getObjectId();
						if (legion.addLegionMember(playerObjId)) {
							addLegionMember(legion, targetPlayer);
							displayLegionMessage(targetPlayer, legion.getCurrentAnnouncement());
							addHistory(legion, targetPlayer.getName(), LegionHistoryType.JOIN);
						} else {
							PacketSendUtility.sendPacket(activePlayer,
									SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_CAN_NOT_ADD_MEMBER_ANY_MORE);
							targetPlayer.resetLegionMember();
						}
					}
				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
					PacketSendUtility.sendPacket(activePlayer,
							SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_HE_REJECTED_INVITATION(targetPlayer.getName()));
				}
			};
			boolean requested = targetPlayer.getResponseRequester()
					.putRequest(SM_QUESTION_WINDOW.STR_GUILD_INVITE_I_JOINED_MSGBOX, responseHandler);
			if (!requested) {
				PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_OTHER_IS_BUSY);
			} else {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_SENT_INVITE_MSG_TO_HIM(targetPlayer.getName()));
				PacketSendUtility.sendPacket(targetPlayer,
						new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_GUILD_INVITE_I_JOINED_MSGBOX, 0, 0,
								legion.getLegionName(), legion.getLegionLevel() + "", activePlayer.getName()));
			}
		}
	}

	/**
	 * 显示当前军团公告。
	 * Displays current legion announcement
	 *
	 * target player
	 * current announcement
	 */
	private void displayLegionMessage(Player targetPlayer, Entry<Timestamp, String> currentAnnouncement) {
		if (currentAnnouncement != null) {
			PacketSendUtility.sendPacket(targetPlayer, SM_SYSTEM_MESSAGE.STR_GUILD_NOTICE(
					currentAnnouncement.getValue(), (int) (currentAnnouncement.getKey().getTime() / 1000)));
		}
	}

	/**
	 * 处理新任命的军团长。
	 * This method will handle a new appointed legion leader
	 *
	 * active player
	 * target player
	 */
	private void appointBrigadeGeneral(final Player activePlayer, final Player targetPlayer) {
		if (legionRestrictions.canAppointBrigadeGeneral(activePlayer, targetPlayer)) {
			final Legion legion = activePlayer.getLegion();
			RequestResponseHandler responseHandler = new RequestResponseHandler(activePlayer) {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					if (!targetPlayer.getCommonData().isOnline()) {
						PacketSendUtility.sendPacket(activePlayer,
								SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_NO_SUCH_USER);
					} else {
						LegionMember legionMember = targetPlayer.getLegionMember();
						if (legionMember.getRank().getRankId() > LegionRank.BRIGADE_GENERAL.getRankId()) {
							// 将旅团将军降为百夫长 / Demote Brigade General to Centurion
							activePlayer.getLegionMember().setRank(LegionRank.CENTURION);
							PacketSendUtility.broadcastPacketToLegion(legion,
									new SM_LEGION_UPDATE_MEMBER(activePlayer, 0, ""));

							// 将成员晋升为旅团将军 / Promote member to Brigade General
							legionMember.setRank(LegionRank.BRIGADE_GENERAL);
							PacketSendUtility.broadcastPacketToLegion(legion,
									new SM_LEGION_UPDATE_MEMBER(targetPlayer, 1300273, targetPlayer.getName()));
							PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x08));
							addHistory(legion, targetPlayer.getName(), LegionHistoryType.APPOINTED);
						}
					}
				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
					PacketSendUtility.sendPacket(activePlayer,
							SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_HE_DECLINE_YOUR_OFFER(targetPlayer.getName()));
				}
			};

			boolean requested = targetPlayer.getResponseRequester()
					.putRequest(SM_QUESTION_WINDOW.STR_GUILD_CHANGE_MASTER_DO_YOU_ACCEPT_OFFER, responseHandler);
			// 若玩家忙碌无法询问 / If the player is busy and could not be asked
			if (!requested) {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_SENT_CANT_OFFER_WHEN_HE_IS_QUESTION_ASKED);
			} else {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_SENT_OFFER_MSG_TO_HIM(targetPlayer.getName()));

				// 向好友发送询问包 / Send question packet to buddy
				PacketSendUtility.sendPacket(targetPlayer,
						new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_GUILD_CHANGE_MASTER_DO_YOU_ACCEPT_OFFER,
								activePlayer.getObjectId(), 0, activePlayer.getName()));
			}
		}
	}

	/**
	 * 处理成员离线时的升降职。
	 * This method will handle the process when a member is demoted or promoted while offline
	 *
	 * active player
	 */
	private void appointRank(Player activePlayer, String charName, int rankId) {
		final LegionMemberEx LM = getLegionMemberEx(charName);
		if (LM == null) {
			log.error(I18n.get("log.10437023e015", charName));
			return;
		}
		if (legionRestrictions.canAppointRank(activePlayer, LM.getObjectId())) {
			Legion legion = activePlayer.getLegion();
			LegionRank rank = LegionRank.values()[rankId];
			int msgId = 0;
			switch (rank) {
			case DEPUTY:
				msgId = 1400902;
				break;
			case LEGIONARY:
				msgId = 1300268;
				break;
			case CENTURION:
				msgId = 1300267;
				break;
			case VOLUNTEER:
				msgId = 1400903;
			}
			LegionMember legionMember = getLegionMember(LM.getObjectId());
			legionMember.setRank(rank);
			DAOManager.getDAO(LegionMemberDAO.class).storeLegionMember(legionMember.getObjectId(), legionMember);
			LM.setRank(rank);
			PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_UPDATE_MEMBER(LM, msgId, LM.getName()));
		}
	}

	/**
	 * 处理成员升降职。
	 * This method will handle the process when a member is demoted or promoted
	 *
	 * active player
	 */
	private void appointRank(Player activePlayer, Player targetPlayer, int rankId) {
		if (legionRestrictions.canAppointRank(activePlayer, targetPlayer.getObjectId())) {
			Legion legion = activePlayer.getLegion();
			int msgId = 0;
			LegionRank rank = LegionRank.values()[rankId];
			LegionMember legionMember = targetPlayer.getLegionMember();
			switch (rank) {
			case DEPUTY:
				msgId = 1400902;
				break;
			case LEGIONARY:
				msgId = 1300268;
				break;
			case CENTURION:
				msgId = 1300267;
				break;
			case VOLUNTEER:
				msgId = 1400903;
			}
			legionMember.setRank(rank);
			PacketSendUtility.broadcastPacketToLegion(legion,
					new SM_LEGION_UPDATE_MEMBER(targetPlayer, msgId, targetPlayer.getName()));
		}
	}

	/**
	 * 处理自我介绍变更。
	 * This method will handle the changement of a self intro
	 *
	 * active player
	 * @param newSelfIntro 新自我介绍 / new self intro
	 */
	private void changeSelfIntro(Player activePlayer, String newSelfIntro) {
		if (legionRestrictions.canChangeSelfIntro(activePlayer, newSelfIntro)) {
			LegionMember legionMember = activePlayer.getLegionMember();
			legionMember.setSelfIntro(newSelfIntro);
			PacketSendUtility.broadcastPacketToLegion(legionMember.getLegion(),
					new SM_LEGION_UPDATE_SELF_INTRO(activePlayer.getObjectId(), newSelfIntro));
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_WRITE_INTRO_DONE);
		}
	}

	/**
	 * 更新军团各职级权限掩码并广播给在线成员。
	 * Updates rank permission masks for the legion and broadcasts to online members.
	 *
	 * Target legion
	 * @param deputyPermission 副团长权限 / Deputy permissions
	 * @param centurionPermission 百夫长权限 / Centurion permissions
	 * @param legionarPermission 军团兵权限 / Legionary permissions
	 * @param volunteerPermission 志愿兵权限 / Volunteer permissions
	 */
	public void changePermissions(Legion legion, short deputyPermission, short centurionPermission,
			short legionarPermission, short volunteerPermission) {
		if (legion.setLegionPermissions(deputyPermission, centurionPermission, legionarPermission,
				volunteerPermission)) {
			PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x02, legion));
		}
	}

	/**
	 * 处理军团升级。
	 * This method will handle the leveling up of a legion
	 *
	 * active player
	 */
	private void requestChangeLevel(Player activePlayer) {
		if (legionRestrictions.canChangeLevel(activePlayer)) {
			Legion legion = activePlayer.getLegion();
			activePlayer.getInventory().decreaseKinah(legion.getKinahPrice());
			changeLevel(legion, legion.getLegionLevel() + 1, false);
			addHistory(legion, legion.getLegionLevel() + "", LegionHistoryType.LEVEL_UP);
		}
	}

	/**
	 * 变更军团等级并通知在线成员；可选立即落库。
	 * Changes the legion level, notifies online members, and optionally persists.
	 *
	 * Target legion
	 * New level
	 * @param save 是否立即保存 / Whether to store immediately
	 */
	public void changeLevel(Legion legion, int newLevel, boolean save) {
		legion.setLegionLevel(newLevel);
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x00, legion));
		PacketSendUtility.broadcastPacketToLegion(legion, SM_SYSTEM_MESSAGE.STR_GUILD_EVENT_LEVELUP(newLevel));
		if (save) {
			storeLegion(legion);
		}
	}

	/**
	 * 处理昵称变更。
	 * This method will handle the changement of a nickname
	 *
	 * active player
	 * character name
	 */
	private void changeNickname(Player activePlayer, String charName, String newNickname) {
		Legion legion = activePlayer.getLegion();
		LegionMember legionMember;
		Player targetPlayer;
		if ((targetPlayer = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(charName)) != null) {
			legionMember = targetPlayer.getLegionMember();
			if (targetPlayer.getLegion() != legion) {
				return;
			}
		} else {
			LegionMemberEx LM = getLegionMemberEx(charName);
			if (LM == null || LM.getLegion() != legion) {
				return;
			}
			legionMember = getLegionMember(LM.getObjectId());
		}
		if (legionRestrictions.canChangeNickname(legion, legionMember.getObjectId(), newNickname)) {
			legionMember.setNickname(newNickname);
			PacketSendUtility.broadcastPacketToLegion(legion,
					new SM_LEGION_UPDATE_NICKNAME(legionMember.getObjectId(), newNickname));
			if (targetPlayer == null) {
				DAOManager.getDAO(LegionMemberDAO.class).storeLegionMember(legionMember.getObjectId(), legionMember);
			}
		}
	}

	/**
	 * 军团解散后从所有在线成员移除军团信息。
	 * This method will remove legion from all legion members online after a legion has been disbanded
	 *
	 * legion
	 */
	private void updateAfterDisbandLegion(Legion legion) {
		for (Player onlineLegionMember : legion.getOnlineLegionMembers()) {
			PacketSendUtility.broadcastPacket(onlineLegionMember,
					new SM_LEGION_UPDATE_TITLE(onlineLegionMember.getObjectId(), 0, "", 0), true);
			PacketSendUtility.sendPacket(onlineLegionMember,
					new SM_LEGION_LEAVE_MEMBER(1300302, 0, legion.getLegionName()));
			onlineLegionMember.resetLegionMember();
		}
	}

	/**
	 * 向每位军团成员发送数据包。
	 * This method will send a packet to every legion member
	 *
	 * legion
	 * emblem type
	 */
	private void updateMembersEmblem(Legion legion, LegionEmblemType emblemType) {
		LegionEmblem legionEmblem = legion.getLegionEmblem();
		for (Player onlineLegionMember : legion.getOnlineLegionMembers()) {
			PacketSendUtility.broadcastPacket(onlineLegionMember,
					new SM_LEGION_UPDATE_EMBLEM(legion.getLegionId(), legionEmblem.getEmblemId(),
							legionEmblem.getColor_r(), legionEmblem.getColor_g(), legionEmblem.getColor_b(),
							emblemType),
					true);
			if (legionEmblem.getEmblemType() == LegionEmblemType.CUSTOM) {
				sendEmblemData(onlineLegionMember, legionEmblem, legion.getLegionId(), legion.getLegionName());
			}
		}
	}

	/**
	 * 向每位军团成员发送数据包并更新解散信息。
	 * This method will send a packet to every legion member and update them about the disband
	 *
	 * legion
	 * unix time
	 */
	private void updateMembersOfDisbandLegion(Legion legion, int unixTime) {
		for (Player onlineLegionMember : legion.getOnlineLegionMembers()) {
			PacketSendUtility.sendPacket(onlineLegionMember,
					new SM_LEGION_UPDATE_MEMBER(onlineLegionMember, 1300303, unixTime + ""));
			PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x06, unixTime));
		}
	}

	/**
	 * 向每位军团成员发送数据包并更新解散信息。
	 * This method will send a packet to every legion member and update them about the disband
	 *
	 * legion
	 */
	private void updateMembersOfRecreateLegion(Legion legion) {
		for (Player onlineLegionMember : legion.getOnlineLegionMembers()) {
			PacketSendUtility.sendPacket(onlineLegionMember,
					new SM_LEGION_UPDATE_MEMBER(onlineLegionMember, 1300307, ""));
			PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x07));
		}
	}

	/**
	 * 保存自定义军团徽章并同步给所有在线成员。
	 * Stores a custom legion emblem and syncs it to all online members.
	 *
	 * Acting player
	 * @param customEmblem 自定义徽章 / Custom emblem
	 */
	public void storeLegionEmblem(Player activePlayer, LegionEmblem customEmblem) {
		addHistory(activePlayer.getLegion(), "", LegionHistoryType.EMBLEM_MODIFIED);
		activePlayer.getLegion().setLegionEmblem(customEmblem);
		updateMembersEmblem(activePlayer.getLegion(), customEmblem.getEmblemType());
		PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_EMBLEM);
	}

	/**
	 * 保存预设/标准军团徽章（扣费、写历史、广播更新）。
	 * Stores a standard/pre 设置军团徽章。
	 * Set legion emblem (charges kinah, writes history, broadcasts update).
	 *
	 * Acting player
	 * Legion id
	 * Emblem template id
	 * @param color_r 红色分量 / Red component
	 * @param color_g 绿色分量 / Green component
	 * @param color_b 蓝色分量 / Blue component
	 * Emblem type
	 */
	public void storeLegionEmblem(Player activePlayer, int legionId, int emblemId, int color_r, int color_g,
			int color_b, LegionEmblemType emblemType) {
		if (legionRestrictions.canStoreLegionEmblem(activePlayer, legionId, emblemId)) {
			Legion legion = activePlayer.getLegion();
			if (legion.getLegionEmblem().isDefaultEmblem()) {
				addHistory(legion, "", LegionHistoryType.EMBLEM_REGISTER);
			} else {
				addHistory(legion, "", LegionHistoryType.EMBLEM_MODIFIED);
			}
			activePlayer.getInventory().decreaseKinah(LegionConfig.LEGION_EMBLEM_REQUIRED_KINAH);
			legion.getLegionEmblem().setEmblem(emblemId, color_r, color_g, color_b, emblemType, null);
			updateMembersEmblem(legion, emblemType);
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_EMBLEM);
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
		ArrayList<LegionMemberEx> legionMembers = new ArrayList<LegionMemberEx>();
		for (Integer memberObjId : legion.getLegionMembers()) {
			LegionMemberEx legionMemberEx;
			if (objExcluded != null && objExcluded.equals(memberObjId)) {
				continue;
			}
			Player memberPlayer = world.findPlayer(memberObjId);
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
	 * 返回军团旅长名称；找不到时返回错误占位串。
	 * Returns the brigade general name, or an error placeholder if missing.
	 *
	 * Target legion
	 * Brigade general name
	 */
	public String getBrigadeGeneralName(Legion legion) {
		for (LegionMemberEx member : loadLegionMemberExList(legion, null)) {
			if (member.isBrigadeGeneral()) {
				return member.getName();
			}
		}
		return "ERROR Name !!!";
	}

	/**
	 * 返回在线的军团旅长玩家对象；离线则为 null。
	 * Returns the online brigade general player, or null if offline/missing.
	 *
	 * Target legion
	 *
	 * @param legion
	 * @return 在线旅长，或 null / Online brigade general, or null
	 */
	public Player getBrigadeGeneral(Legion legion) {
		Player player = null;
		for (LegionMemberEx member : loadLegionMemberExList(legion, null)) {
			if (member.isBrigadeGeneral()) {
				player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(member.getObjectId());
			}
		}
		return player;
	}

	/**
	 * 打开军团仓库：校验权限、同步仓库数据并发送物品/对话框包。
	 * Opens the legion warehouse: validates access, syncs data, and sends item/dialog packets.
	 *
	 * Acting player
	 * Warehouse NPC
	 */
	public void openLegionWarehouse(Player player, Npc npc) {
		if (legionRestrictions.canOpenWarehouse(player)) {
			LegionWhUpdate(player);
			PacketSendUtility.sendPacket(player, new SM_LEGION_EDIT(0x04, player.getLegion()));// kinah
			int whLvl = player.getLegion().getWarehouseLevel();
			List<Item> items = player.getLegion().getLegionWarehouse().getItems();
			int storageId = StorageType.LEGION_WAREHOUSE.getId();
			boolean isEmpty = items.isEmpty();
			if (!isEmpty) {
				ListSplitter<Item> splitter = new ListSplitter<Item>(items, 10);
				while (!splitter.isLast()) {
					PacketSendUtility.sendPacket(player,
							new SM_WAREHOUSE_INFO(splitter.getNext(), storageId, whLvl, splitter.isFirst(), player));
				}
			}
			PacketSendUtility.sendPacket(player, new SM_WAREHOUSE_INFO(null, storageId, whLvl, isEmpty, player));
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npc.getObjectId(), 25));
		}
	}

	/**
	 * 取消进行中的解散并恢复军团（需旅长确认）。
	 * Cancels an in-progress disband and recreates/restores the legion (brigade general confirm).
	 *
	 * Triggering NPC
	 * Acting player
	 */
	public void recreateLegion(Npc npc, Player activePlayer) {
		final Legion legion = activePlayer.getLegion();
		if (legionRestrictions.canRecreateLegion(activePlayer, legion)) {
			RequestResponseHandler disbandResponseHandler = new RequestResponseHandler(npc) {
				@Override
				public void acceptRequest(Creature requester, Player responder) {
					legion.setDisbandTime(0);
					PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x07));
					updateMembersOfRecreateLegion(legion);
				}

				@Override
				public void denyRequest(Creature requester, Player responder) {
					// 无消息 / no message
				}
			};

			boolean disbandResult = activePlayer.getResponseRequester()
					.putRequest(SM_QUESTION_WINDOW.STR_GUILD_DISPERSE_STAYMODE_CANCEL, disbandResponseHandler);
			if (disbandResult) {
				PacketSendUtility.sendPacket(activePlayer,
						new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_GUILD_DISPERSE_STAYMODE_CANCEL, 0, 0));
			}
		}
	}

	/**
	 * 根据新排行表刷新已缓存军团的排名并广播编辑包。
	 * Refreshes ranks of cached legions from the new ranking map and broadcasts edit packets.
	 *
	 * Map of legion id to rank
	 */
	public void performRankingUpdate(Map<Integer, Integer> legionRanking) {
		log.info(I18n.get("log.63db4fabda94"));
		long startTime = System.currentTimeMillis();

		Iterator<Legion> legionsIterator = allCachedLegions.iterator();
		int legionsUpdated = 0;

		this.legionRanking = legionRanking;

		while (legionsIterator.hasNext()) {
			Legion legion = legionsIterator.next();
			if (legionRanking.containsKey(legion.getLegionId())) {
				legion.setLegionRank(legionRanking.get(legion.getLegionId()));
				PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x01, legion));
			}
			legionsUpdated++;
		}
		long workTime = System.currentTimeMillis() - startTime;
		log.info(I18n.get("log.b26e20796658", workTime, legionsUpdated));
	}

	/**
	 * 将玩家所属军团仓库的物品与魔石持久化到数据库。
	 * Persists the player legion warehouse items and item stones to the database.
	 *
	 * @param player 触发同步的玩家 / Player triggering the warehouse sync
	 */
	public void LegionWhUpdate(Player player) {
		Legion legion = player.getLegion();

		if (legion == null) {
			return;
		}
		List<Item> allItems = legion.getLegionWarehouse().getItemsWithKinah();
		allItems.addAll(legion.getLegionWarehouse().getDeletedItems());
		try {
			/**
	 * 1. 先保存物品。
	 * 1. save items first
	 */
			DAOManager.getDAO(InventoryDAO.class).store(allItems, player.getObjectId(),
					player.getPlayerAccount().getId(), legion.getLegionId());

			/**
	 * 2. 保存物品镶嵌石。
	 * 2. save item stones
	 */
			DAOManager.getDAO(ItemStoneListDAO.class).save(allItems);
		} catch (Exception ex) {
			log.error(I18n.get("log.ea0f9e89569d", ex), ex);
		}
	}

	/**
	 * 向军团广播成员信息更新（等级/职业等变化）。
	 * Broadcasts a member info update (level/class changes, etc.) to the legion.
	 *
	 * @param player 发生变化的成员 / Changed member
	 */
	public void updateMemberInfo(Player player) {
		PacketSendUtility.broadcastPacketToLegion(player.getLegion(), new SM_LEGION_UPDATE_MEMBER(player, 0, ""));
	}

	/**
	 * 设置军团贡献点（常用于管理指令），并可选落库。
	 * Sets legion contribution points (often via admin command) and optionally persists.
	 *
	 * Target legion
	 * New contribution points
	 * @param save 是否立即保存 / Whether to store immediately
	 */
	public void setContributionPoints(Legion legion, long newPoints, boolean save) {
		legion.setContributionPoints(newPoints);
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_EDIT(0x03, legion));
		if (save) {
			storeLegion(legion);
		}
	}

	/**
	 * 开始上传自定义徽章：记录颜色/类型与总字节数并进入上传中状态。
	 * Starts custom emblem upload: records colors/type and total size, marks uploading.
	 *
	 * Acting player
	 * @param totalSize 徽章数据总大小 / Total emblem data size
	 * @param color_r 红色分量 / Red component
	 * @param color_g 绿色分量 / Green component
	 * @param color_b 蓝色分量 / Blue component
	 * Emblem type
	 */
	public void uploadEmblemInfo(Player activePlayer, int totalSize, int color_r, int color_g, int color_b,
			LegionEmblemType emblemType) {
		if (legionRestrictions.canUploadEmblemInfo(activePlayer)) {
			LegionEmblem legionEmblem = activePlayer.getLegion().getLegionEmblem();
			legionEmblem.resetUploadSettings();

			int emblemId = legionEmblem.getEmblemId() + 1;
			legionEmblem.setEmblem(emblemId, color_r, color_g, color_b, emblemType, null);
			legionEmblem.setUploadSize(totalSize);
			legionEmblem.setUploading(true);
		}
	}

	/**
	 * 接收自定义徽章分片数据；收齐后扣费并落库生效。
	 * Receives a chunk of custom emblem data; when complete, charges kinah and persists the emblem.
	 *
	 * Acting player
	 * @param size 本片字节数 / Chunk size
	 * @param data 本片数据 / Chunk bytes
	 */
	public void uploadEmblemData(Player activePlayer, int size, byte[] data) {
		if (legionRestrictions.canUploadEmblem(activePlayer)) {
			LegionEmblem legionEmblem = activePlayer.getLegion().getLegionEmblem();
			legionEmblem.addUploadedSize(size);
			legionEmblem.addUploadData(data);

			if (legionEmblem.getUploadSize() == legionEmblem.getUploadedSize()) {
				if (legionEmblem.getUploadedSize() == 0 || legionEmblem.getUploadSize() == 0) {
					PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_WARN_CORRUPT_EMBLEM_FILE);
					return;
				}
				if (!activePlayer.getInventory().tryDecreaseKinah(1130000)) {
					PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY);
					return;
				}
				// 已完成 / Finished
				legionEmblem.setCustomEmblemData(legionEmblem.getUploadData());
				DAOManager.getDAO(LegionDAO.class).storeLegionEmblem(activePlayer.getLegion().getLegionId(),
						legionEmblem);
				LegionEmblem emblem = DAOManager.getDAO(LegionDAO.class)
						.loadLegionEmblem(activePlayer.getLegion().getLegionId());
				storeLegionEmblem(activePlayer, emblem);
			}
		}
	}

	/**
	 * 向玩家分包发送自定义徽章二进制数据。
	 * Sends custom emblem binary data to a player in packets.
	 *
	 * Receiving player
	 * Emblem object
	 * Legion id
	 * Legion name
	 */
	public void sendEmblemData(Player player, LegionEmblem legionEmblem, int legionId, String legionName) {
		PacketSendUtility.sendPacket(player,
				new SM_LEGION_SEND_EMBLEM(legionId, legionEmblem.getEmblemId(), legionEmblem.getColor_r(),
						legionEmblem.getColor_g(), legionEmblem.getColor_b(), legionName, legionEmblem.getEmblemType(),
						legionEmblem.getCustomEmblemData().length));
		ByteBuffer buf = ByteBuffer.allocate(legionEmblem.getCustomEmblemData().length);
		buf.put(legionEmblem.getCustomEmblemData()).position(0);
		log.debug("legionEmblem size: " + buf.capacity() + " bytes");
		int maxSize = 7993;
		int currentSize;
		byte[] bytes;
		do {
			log.debug("legionEmblem data position: " + buf.position());
			currentSize = buf.capacity() - buf.position();
			log.debug("legionEmblem data remaining capacity: " + currentSize + " bytes");

			if (currentSize >= maxSize) {
				bytes = new byte[maxSize];
				for (int i = 0; i < maxSize; i++) {
					bytes[i] = buf.get();
				}
				log.debug("legionEmblem data send size: " + (bytes.length) + " bytes");
				PacketSendUtility.sendPacket(player, new SM_LEGION_SEND_EMBLEM_DATA(maxSize, bytes));
			} else {
				bytes = new byte[currentSize];
				for (int i = 0; i < currentSize; i++) {
					bytes[i] = buf.get();
				}
				log.debug("legionEmblem data send size: " + (bytes.length) + " bytes");
				PacketSendUtility.sendPacket(player, new SM_LEGION_SEND_EMBLEM_DATA(currentSize, bytes));
			}
		} while (buf.capacity() != buf.position());
	}

	/**
	 * 重命名军团并刷新在线成员称号显示；可选落库。
	 * Renames the legion and refreshes online member titles; optionally persists.
	 *
	 * Target legion
	 * New name
	 * @param save 是否立即保存 / Whether to store immediately
	 */
	public void setLegionName(Legion legion, String newLegionName, boolean save) {
		legion.setLegionName(newLegionName);
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_INFO(legion));

		for (Player legionMember : legion.getOnlineLegionMembers()) {
			PacketSendUtility
					.broadcastPacket(legionMember,
							new SM_LEGION_UPDATE_TITLE(legionMember.getObjectId(), legion.getLegionId(),
									legion.getLegionName(), legionMember.getLegionMember().getRank().getRankId()),
							true);
		}
		if (save) {
			storeLegion(legion);
		}
	}

	/**
	 * 向数据库添加新公告并更新当前公告。
	 * This will add a new announcement to the DB and change the current announcement
	 *
	 * active player
	 * announcement
	 */
	private void changeAnnouncement(Player activePlayer, String announcement) {
		if (legionRestrictions.canChangeAnnouncement(activePlayer.getLegionMember(), announcement)) {
			Legion legion = activePlayer.getLegion();

			Timestamp currentTime = new Timestamp(System.currentTimeMillis());
			storeNewAnnouncement(legion.getLegionId(), currentTime, announcement);
			legion.addAnnouncementToList(currentTime, announcement);
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_WRITE_NOTICE_DONE);
			PacketSendUtility.broadcastPacketToLegion(legion,
					new SM_LEGION_EDIT(0x05, (int) (System.currentTimeMillis() / 1000), announcement));
		}
	}

	/**
	 * 存储全部军团公告。
	 * This method stores all legion announcements
	 *
	 * legion
	 */
	private void storeLegionAnnouncements(Legion legion) {
		for (int i = 0; i < (legion.getAnnouncementList().size() - 7); i++) {
			removeAnnouncement(legion.getLegionId(), legion.getAnnouncementList().firstEntry().getKey());
			legion.removeFirstEntry();
		}
	}

	/**
	 * 存储新创建的公告。
	 * Stores newly created announcement
	 *
	 * legion id
	 * current time
	 * message
	 *
	 * @return true if announcement was successful saved.
	 */
	private boolean storeNewAnnouncement(int legionId, Timestamp currentTime, String message) {
		return DAOManager.getDAO(LegionDAO.class).saveNewAnnouncement(legionId, currentTime, message);
	}

	/**
	 * 军团服务辅助方法。
	 * Legion service helper.
	 *
	 * @param legionId
	 * @param key
	 * @return true if succeeded
	 */
	private void removeAnnouncement(int legionId, Timestamp key) {
		DAOManager.getDAO(LegionDAO.class).removeAnnouncement(legionId, key);
	}

	private void addHistory(Legion legion, String text, LegionHistoryType legionHistoryType) {
		addHistory(legion, text, legionHistoryType, 0, StringUtils.EMPTY);
	}

	/**
	 * 追加军团历史记录并广播对应页签更新。
	 * Appends a legion history entry and broadcasts the related tab update.
	 *
	 * Target legion
	 * @param text 历史文本 / History text
	 * History type
	 * Tab id
	 * Extra description
	 */
	public void addHistory(Legion legion, String text, LegionHistoryType legionHistoryType, int tabId,
			String description) {
		LegionHistory legionHistory = new LegionHistory(legionHistoryType, text,
				new Timestamp(System.currentTimeMillis()), tabId, description);

		legion.addHistory(legionHistory);
		DAOManager.getDAO(LegionDAO.class).saveNewLegionHistory(legion.getLegionId(), legionHistory);

		PacketSendUtility.broadcastPacketToLegion(legion,
				new SM_LEGION_TABS(legion.getLegionHistoryByTabId(tabId), tabId));
	}

	/**
	 * 以志愿兵军阶将新成员加入军团。
	 * This method will add a new legion member to a legion with VOLUNTEER rank
	 *
	 * legion
	 * 玩家 / player
	 */
	private void addLegionMember(Legion legion, Player player) {
		addLegionMember(legion, player, LegionRank.VOLUNTEER);
	}

	/**
	 * 以指定军阶将新成员加入军团。
	 * This method will add a new legion member to a legion with input rank
	 *
	 * legion
	 * 玩家 / player
	 * rank
	 */
	private void addLegionMember(Legion legion, Player player, LegionRank rank) {
		player.setLegionMember(new LegionMember(player.getObjectId(), legion, rank));
		storeLegionMember(player.getLegionMember(), true);
		PacketSendUtility.sendPacket(player, new SM_LEGION_INFO(legion));
		ArrayList<LegionMemberEx> totalMembers = loadLegionMemberExList(legion, player.getObjectId());
		ListSplitter<LegionMemberEx> splits = new ListSplitter<LegionMemberEx>(totalMembers, 128);
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
	private boolean removeLegionMember(String charName, boolean kick, String playerName) {
		/**
	 * 从缓存获取 LegionMemberEx，离线则读库。
	 * Get LegionMemberEx from cache or database if offline
	 */
		LegionMemberEx legionMember = getLegionMemberEx(charName);
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
		Player player = world.findPlayer(charName);
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
	 * 处理与角色名相关的军团请求（邀请、踢人、任命旅长/职级、改昵称）。
	 * Handles character-name based legion requests (invite, kick, appoint ranks, nickname).
	 *
	 * @param exOpcode 扩展操作码 / Extended opcode
	 * Acting player
	 * @param charName 目标角色名 / Target character name
	 * @param newNickname 新昵称（改昵称时） / New nickname (when renaming)
	 * @param rank 新职级（任命时） / New rank (when appointing)
	 */
	public void handleCharNameRequest(int exOpcode, Player activePlayer, String charName, String newNickname,
			int rank) {
		Legion legion = activePlayer.getLegion();

		charName = Util.convertName(charName);
		Player targetPlayer = world.findPlayer(charName);

		switch (exOpcode) {
		/**
	 * 邀请加入军团。
	 * Invite to legion
	 */
		case 0x01:
			if (targetPlayer != null) {
				if (targetPlayer.getPlayerSettings().isInDeniedStatus(DeniedStatus.GUILD)) {
					PacketSendUtility.sendPacket(activePlayer,
							SM_SYSTEM_MESSAGE.STR_MSG_REJECTED_INVITE_GUILD(charName));
					return;
				}
				invitePlayerToLegion(activePlayer, targetPlayer);
			} else {
				PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_NO_USER_TO_INVITE);
			}
			break;
		/**
	 * 将成员踢出军团。
	 * Kick member from legion
	 */
		case LEGION_ACTION_KICK:
			/**
	 * 检查玩家是否可被踢出军团。 / Check whether the player can be kicked from the legion.
	 */
			if (legionRestrictions.canKickPlayer(activePlayer, charName)) {
				if (removeLegionMember(charName, true, activePlayer.getName())) {
					// 向成员发送数据包？ / send packet to members?
					if (targetPlayer != null) {
						PacketSendUtility.sendPacket(targetPlayer,
								new SM_LEGION_LEAVE_MEMBER(1300246, 0, legion.getLegionName()));
						targetPlayer.resetLegionMember();
					}
				}
			}
			if (legion.hasBonus()) {
				PacketSendUtility.sendPacket(activePlayer, new SM_ICON_INFO(1, false));
			}
			break;
		/**
	 * 任命新军团长。
	 * Appoint a new Brigade General
	 */
		case 0x05:
			if (targetPlayer != null) {
				appointBrigadeGeneral(activePlayer, targetPlayer);
			} else {
				PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_NO_SUCH_USER);
			}
			break;
		/**
	 * 任命百夫长/军团兵。
	 * Appoint Centurion/Legionairy
	 */
		case 0x06:
			if (targetPlayer != null)
				appointRank(activePlayer, targetPlayer, rank);
			else
				appointRank(activePlayer, charName, rank);
			break;
		/**
	 * 设置昵称。
	 * Set nickname
	 */
		case 0x0F:
			changeNickname(activePlayer, charName, newNickname);
			break;
		}
	}

	/**
	 * 处理带文本的军团请求（公告、自我介绍）。
	 * Handles text-bearing legion requests (announcement, self intro).
	 *
	 * @param exOpcode 扩展操作码 / Extended opcode
	 * Acting player
	 * @param text 文本内容 / Text payload
	 */
	public void handleLegionRequest(int exOpcode, Player activePlayer, String text) {
		switch (exOpcode) {
		/**
	 * 编辑公告。
	 * Edit announcements
	 */
		case 0x09:
			changeAnnouncement(activePlayer, text);
			break;
		/**
	 * 修改自我介绍。
	 * Change self introduction
	 */
		case 0x0A:
			changeSelfIntro(activePlayer, text);
			break;
		}
	}

	/**
	 * 处理无文本的军团请求（退团、升级）。
	 * Handles textless legion requests (leave, level up).
	 *
	 * @param exOpcode 扩展操作码 / Extended opcode
	 * Acting player
	 */
	public void handleLegionRequest(int exOpcode, Player activePlayer) {
		switch (exOpcode) {
		/**
	 * 离开军团。
	 * Leave legion
	 */
		case 0x02:
			if (legionRestrictions.canLeave(activePlayer)) {
				if (removeLegionMember(activePlayer.getName(), false, "")) {
					Legion legion = activePlayer.getLegion();
					PacketSendUtility.sendPacket(activePlayer,
							new SM_LEGION_LEAVE_MEMBER(1300241, 0, legion.getLegionName()));
					activePlayer.resetLegionMember();
					if (legion.hasBonus()) {
						PacketSendUtility.sendPacket(activePlayer, new SM_ICON_INFO(1, false));
					}
				}
			}
			break;
		/**
	 * 提升军团等级。
	 * Level legion up
	 */
		case 0x0E:
			requestChangeLevel(activePlayer);
			break;
		}
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
	 * Logging-in player
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
		ListSplitter<LegionMemberEx> splits = new ListSplitter<LegionMemberEx>(totalMembers, 128);
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
		displayLegionMessage(activePlayer, legion.getCurrentAnnouncement());
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
	 * Logging-out player
	 */
	public void onLogout(Player player) {
		Legion legion = player.getLegion();
		LegionWarehouse lwh = player.getLegion().getLegionWarehouse();
		if (lwh.getWhUser() == player.getObjectId()) {
			lwh.setWhUser(0);
		}
		PacketSendUtility.broadcastPacketToLegion(legion, new SM_LEGION_UPDATE_MEMBER(player));
		storeLegion(legion);
		storeLegionMember(player.getLegionMember());
		storeLegionMemberExInCache(player);
		storeLegionAnnouncements(legion);
		legion.removeBonus();
	}

	/**
	 * 清空军团与成员缓存容器。
	 * Clears the legion and legion-member caches.
	 */
	public void clearCaches() {
		allCachedLegions.clear();
		allCachedLegionMembers.clear();
	}

	/**
	 * 军团功能限制校验集合，封装创建/邀请/踢人/权限/仓库等前置条件。
	 * Restriction checks for legion features: create, invite, kick, rights, warehouse, etc.
	 *
	 * @author Simple
	 */
	private class LegionRestrictions {

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
		private boolean canCreateLegion(Player activePlayer, String legionName, Npc creatorNpc) {
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
		private boolean canInvitePlayer(Player activePlayer, Player targetPlayer) {
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
			} else if (!activePlayer.getLegionMember().hasRights(LegionPermissionsMask.INVITE)) {
				// 无权邀请 / No rights to invite
				return false;
			} else if (activePlayer.getRace() != targetPlayer.getRace() && !LegionConfig.LEGION_INVITEOTHERFACTION) {
				// 不同种族 / Not Same Race
				return false;
			}
			return true;
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
		private boolean canKickPlayer(Player activePlayer, String charName) {
			/**
	 * 从缓存获取 LegionMemberEx，离线则读库。
	 * Get LegionMemberEx from cache or database if offline
	 */
			LegionMemberEx legionMember = getLegionMemberEx(charName);
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
		private boolean canAppointBrigadeGeneral(Player activePlayer, Player targetPlayer) {
			Legion legion = activePlayer.getLegion();
			if (!isBrigadeGeneral(activePlayer)) {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MEMBER_RANK_DONT_HAVE_RIGHT);
				return false;
			}
			if (isSelf(activePlayer, targetPlayer.getObjectId())) {
				PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_ERROR_SELF);
				return false;
			} else if (!legion.isMember(targetPlayer.getObjectId())) {
				// 不在同一军团 / not in same legion
				return false;
			}
			return true;
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
		private boolean canAppointRank(Player activePlayer, int targetObjId) {
			Legion legion = activePlayer.getLegion();
			if (!isBrigadeGeneral(activePlayer)) {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MEMBER_RANK_DONT_HAVE_RIGHT);
				return false;
			}
			if (isSelf(activePlayer, targetObjId)) {
				PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_GUILD_CHANGE_MASTER_ERROR_SELF);
				return false;
			} else if (!legion.isMember(targetObjId)) {
				// 不在同一军团 / not in same legion
				return false;
			}
			return true;
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
		private boolean canChangeSelfIntro(Player activePlayer, String newSelfIntro) {
			if (!isValidSelfIntro(newSelfIntro)) {
				return false;
			}
			return true;
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
		private boolean canChangeLevel(Player activePlayer) {
			Legion legion = activePlayer.getLegion();
			int levelContributionPrice = legion.getContributionPrice();

			if (legion.getLegionLevel() == MAX_LEGION_LEVEL) {
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
		private boolean canChangeNickname(Legion legion, int targetObjectId, String newNickname) {
			if (!isValidNickname(newNickname)) {
				// 无效昵称 / invalid nickname
				return false;
			} else if (!legion.isMember(targetObjectId)) {
				// 不在同一军团 / not in same legion
				return false;
			}
			return true;
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
		private boolean canChangeAnnouncement(LegionMember legionMember, String announcement) {
			return legionMember.hasRights(LegionPermissionsMask.EDIT)
					&& (announcement.isEmpty() ? true : isValidAnnouncement(announcement));
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
		private boolean canDisbandLegion(Player activePlayer, Legion legion) {
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
		private boolean canLeave(Player activePlayer) {
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
			if (!isBrigadeGeneral(activePlayer)) {
				return false;
			}
			return true;
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
		private boolean canRecreateLegion(Player activePlayer, Legion legion) {
			if (!isBrigadeGeneral(activePlayer)) {
				PacketSendUtility.sendPacket(activePlayer,
						SM_SYSTEM_MESSAGE.STR_GUILD_DISPERSE_ONLY_MASTER_CAN_DISPERSE);
				return false;
			} else if (!legion.isDisbanding()) {
				// 军团未在解散 / Legion is not disbanding
				return false;
			}
			return true;
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
		private boolean canUploadEmblemInfo(Player activePlayer) {
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
		private boolean canUploadEmblem(Player activePlayer) {
			if (!isBrigadeGeneral(activePlayer)) {
				// 不是军团长 / Not legion leader
				return false;
			} else if (activePlayer.getLegion().getLegionLevel() < 3) {
				// 军团等级不够高 / Legion level isn't high enough
				return false;
			} else if (!activePlayer.getLegion().getLegionEmblem().isUploading()) {
				// 未上传徽章 / Not uploading emblem
				return false;
			}
			return true;
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
				// Not a valid emblemId
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
	}

	/**
	 * 记录军团仓库存取物品历史（存入/取出）。
	 * Records legion warehouse item deposit/withdraw history.
	 *
	 * Acting player
	 * Item template id
	 * Count
	 * Source storage
	 * Destination storage
	 */
	public void addWHItemHistory(Player player, int itemId, long count, IStorage sourceStorage, IStorage destStorage) {
		Legion legion = player.getLegion();
		if (legion != null) {
			String description = Integer.toString(itemId) + ":" + Long.toString(count);
			if (sourceStorage.getStorageType() == StorageType.LEGION_WAREHOUSE) {
				addHistory(legion, player.getName(), LegionHistoryType.ITEM_WITHDRAW, 2,
						description);
			} else if (destStorage.getStorageType() == StorageType.LEGION_WAREHOUSE) {
				addHistory(legion, player.getName(), LegionHistoryType.ITEM_DEPOSIT, 2,
						description);
			}
		}
	}

	/**
	 * 处理军团搜索：type=0 全量缓存，type=1 按名称模糊匹配。
	 * Handles legion search: type 0 all cached, type 1 name contains filter.
	 *
	 * Requesting player
	 * @param type 搜索类型 / Search type
	 * @param legionName 名称关键字 / Name keyword
	 */
	public void handleLegionSearch(Player player, int type, String legionName) {
		List<Legion> matchingLegions = new ArrayList<>();
		switch (type) {
		case 0:
			matchingLegions = allCachedLegions.getAllLegions();
			break;
		case 1:
			for (Legion legion : allCachedLegions.getAllLegions()) {
				if (legion.getLegionName().toLowerCase().contains(legionName.toLowerCase())) {
					matchingLegions.add(legion);
				}
			}
			break;
		}
		PacketSendUtility.sendPacket(player, new SM_LEGION_SEARCH(matchingLegions));
	}

	/**
	 * 设置军团入团说明（仅旅长），并同步客户端与数据库。
	 * Sets the legion join description (brigade general only) and syncs client/DB.
	 *
	 * Acting player
	 * Join description
	 */
	public void setJoinDescription(Player player, String description) {
		Legion legion = player.getLegion();
		if (legion == null) {
			return;
		}
		if (legionRestrictions.canChangeLegionJoinSetting(player)) {
			legion.setDescription(description);
			PacketSendUtility.sendPacket(player, new SM_LEGION_EDIT(0x0C, legion));
			DAOManager.getDAO(LegionDAO.class).updateLegionDescription(legion);
		}
	}

	/**
	 * 设置军团入团类型（仅旅长），并同步客户端与数据库。
	 * Sets the legion join type (brigade general only) and syncs client/DB.
	 *
	 * Acting player
	 * Join type
	 */
	public void setJoinType(Player player, int joinType) {
		Legion legion = player.getLegion();
		if (legion == null) {
			return;
		}
		if (legionRestrictions.canChangeLegionJoinSetting(player)) {
			legion.setJoinType(joinType);
			PacketSendUtility.sendPacket(player, new SM_LEGION_EDIT(0x0D, legion));
			DAOManager.getDAO(LegionDAO.class).updateLegionDescription(legion);
		}
	}

	/**
	 * 设置入团最低等级（仅旅长），并同步客户端与数据库。
	 * Sets the minimum join level (brigade general only) and syncs client/DB.
	 *
	 * Acting player
	 * Minimum level
	 */
	public void setJoinMinLevel(Player player, int minLevel) {
		Legion legion = player.getLegion();
		if (legion == null) {
			return;
		}
		if (legionRestrictions.canChangeLegionJoinSetting(player)) {
			legion.setMinJoinLevel(minLevel);
			PacketSendUtility.sendPacket(player, new SM_LEGION_EDIT(0x0E, legion));
			DAOManager.getDAO(LegionDAO.class).updateLegionDescription(legion);
		}
	}

	/**
	 * 向玩家发送当前入团申请对应的军团信息包。
	 * Sends the join-request legion info packet to the player.
	 *
	 * Target player
	 * @param legionId 军团 ID，<=0 表示清空 / Legion id, <=0 clears
	 */
	public void sendLegionJoinRequestPacket(Player player, int legionId) {
		if (legionId <= 0) {
			PacketSendUtility.sendPacket(player, new SM_LEGION_REQUEST_INFO(0, ""));
		} else {
			Legion legion = getLegion(legionId);
			PacketSendUtility.sendPacket(player,
					new SM_LEGION_REQUEST_INFO(legion.getLegionId(), legion.getLegionName()));
		}
	}

	/**
	 * 玩家进世界时，按 CommonData 中的申请军团 ID 重发入团申请信息包。
	 * On enter-world, resends join-request info using the legion id stored in CommonData.
	 *
	 * Target player
	 */
	public void sendLegionJoinRequestPacketonEnterWorld(Player player) {
		int legionId = player.getCommonData().getJoinRequestLegionId();
		if (legionId <= 0) {
			PacketSendUtility.sendPacket(player, new SM_LEGION_REQUEST_INFO(0, ""));
		} else {
			Legion legion = getLegion(legionId);
			PacketSendUtility.sendPacket(player,
					new SM_LEGION_REQUEST_INFO(legion.getLegionId(), legion.getLegionName()));
		}
	}

	/**
	 * 处理玩家入团申请：申请入队、直接加入或拒绝招募。
	 * Handles a player join request: apply, direct join, or reject if not recruiting.
	 *
	 * Applying player
	 * Target legion id
	 * Join type
	 * Application message
	 */
	public void handleLegionJoinRequest(Player player, int legionId, int joinType, String joinRequestMsg) {
		Legion legion = getLegion(legionId);
		if (legion == null) {
			return;
		}
		switch (joinType) {
		case 0:
			player.getCommonData().setJoinRequestLegionId(legionId);
			sendLegionJoinRequestPacket(player, legionId);
			LegionJoinRequest ljr = new LegionJoinRequest(legionId, player, joinRequestMsg);
			legion.addJoinRequest(ljr);
			DAOManager.getDAO(LegionDAO.class).storeLegionJoinRequest(ljr);
			player.getCommonData().setJoinRequestLegionId(legionId);
			Player brigadeGeneral = getBrigadeGeneral(legion);
			if (brigadeGeneral != null) {
				PacketSendUtility.sendPacket(brigadeGeneral, new SM_LEGION_REQUEST_PLAYER(ljr));
			}
			break;
		case 1:
			directAddPlayer(legion, player);
			break;
		default:
			PacketSendUtility.sendMessage(player, "This Legion isn't recruiting new members..");
			break;
		}
	}

	/**
	 * 取消玩家对指定军团的入团申请，并通知旅长。
	 * Cancels the player join request for a legion and notifies the brigade general.
	 *
	 * Applying player
	 * Legion id
	 */
	public void handleJoinRequestCancel(Player player, int legionId) {
		Legion legion = getLegion(legionId);
		player.clearJoinRequest();
		sendLegionJoinRequestPacket(player, 0);
		if (legion.getJoinRequestMap().containsKey(player.getObjectId())) {
			legion.getJoinRequestMap().remove(player.getObjectId());
		}
		Player bg = getBrigadeGeneral(legion);
		if (bg != null) {
			PacketSendUtility.sendPacket(bg, new SM_LEGION_REQUEST(player.getObjectId(), false));
		}
	}

	/**
	 * 玩家侧处理入团申请结果（接受则入团，拒绝则清理申请）。
	 * Applies join-request answer on the player side (join on accept, clear on deny).
	 *
	 * Applying player
	 */
	public void handleJoinRequestGetAnswer(Player player) {
		PlayerCommonData pcd = player.getCommonData();
		switch (pcd.getJoinRequestState()) {
		case ACCEPTED:
			if (!player.isOnAStation()) {
				directAddPlayer(pcd.getJoinRequestLegionId(), player);
				handleJoinRequestCancel(player, player.getCommonData().getJoinRequestLegionId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LEGION_APPLICATION_ACCEPTED);
			} else {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LEGION_JOIN_SERVER_CHANGE);
			}
			break;
		case DENIED:
			handleJoinRequestCancel(player, player.getCommonData().getJoinRequestLegionId());
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LEGION_APPLICATION_DENIED);
			break;
		default:
			break;
		}
	}

	/**
	 * 旅长批复入团申请：在线则即时处理，离线则写库状态。
	 * Brigade general answers a join request: handles online immediately or persists offline state.
	 *
	 * Brigade general player
	 * Applicant object id
	 * Whether accepted
	 */
	public void handleJoinRequestGiveAnswer(Player brigadeGeneral, int playerId, boolean accept) {
		boolean playerOnline = true;
		LegionJoinRequestState state = accept ? LegionJoinRequestState.ACCEPTED : LegionJoinRequestState.DENIED;
		Legion legion = brigadeGeneral.getLegion();
		if (legion == null) {
			return;
		}
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerId);
		if (player == null) {
			playerOnline = false;
			DAOManager.getDAO(PlayerDAO.class).updateLegionJoinRequestState(playerId, state);
			if (legion.getJoinRequestMap().containsKey(playerId)) {
				legion.getJoinRequestMap().remove(playerId);
			}
		}
		PacketSendUtility.sendPacket(brigadeGeneral, new SM_LEGION_REQUEST(playerId, accept));
		if (playerOnline) {
			player.getCommonData().setJoinRequestState(state);
			handleJoinRequestGetAnswer(player);
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final LegionService instance = new LegionService();
	}
}
