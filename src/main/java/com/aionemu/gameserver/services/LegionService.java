package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.LegionConfig;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.DeniedStatus;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.model.team.legion.LegionHistory;
import com.aionemu.gameserver.model.team.legion.LegionHistoryType;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequestState;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.model.team.legion.LegionRank;
import com.aionemu.gameserver.model.team.legion.LegionWarehouse;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ICON_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_ADD_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_MEMBERLIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_REQUEST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_REQUEST_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_REQUEST_PLAYER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_SEARCH;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_SEND_EMBLEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_SEND_EMBLEM_DATA;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_TABS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_EMBLEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_NICKNAME;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_SELF_INTRO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_TITLE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.collections.ListSplitter;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.container.LegionContainer;
import com.aionemu.gameserver.world.container.LegionMemberContainer;

import java.util.ArrayList;
import java.util.List;

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
	private final World world;
	/** 踢出成员的军团动作操作码 / Legion action opcode for kicking a member. */
	public final static int LEGION_ACTION_KICK = 4;
	/** 军团最高等级 / Maximum legion level. */
	static final int MAX_LEGION_LEVEL = 8;
	/** 军团排行缓存 / Legion ranking cache. */
	private Map<Integer, Integer> legionRanking;
	/** 军团操作限制校验器 / Legion operation restriction checker. */
	/**
	 * 惰性构造的判权集合，避免构造期暴露 this。
	 * Lazily built permission checker, which keeps {@code this} out of the constructor.
	 */
	private LegionRestrictions legionRestrictions;

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 *
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static LegionService getInstance() {
		ObjectProvider<LegionService> provider = instanceProvider;
		LegionService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("LegionService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
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
	void storeLegion(Legion legion) {
		storeLegion(legion, false);
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
				legionMembers().addCachedLegionMember(legionMember);
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
			this.allCachedLegionMembers.remove(legionMembers().getLegionMemberEx(memberObjId));
		}
		GameFeatureServices.siegeService().cleanLegionId(legion.getLegionId());
		updateAfterDisbandLegion(legion);
		deleteLegionFromDB(legion);
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
		if (restrictions().canDisbandLegion(activePlayer, legion)) {
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
		if (restrictions().canCreateLegion(activePlayer, legionName, creatorNpc)) {
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
			legionMembers().addLegionMember(legion, activePlayer, LegionRank.BRIGADE_GENERAL);
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
			legionMembers().addLegionMember(legion, player);

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
		if (restrictions().canInvitePlayer(activePlayer, targetPlayer)) {
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
							legionMembers().addLegionMember(legion, targetPlayer);
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
	void displayLegionMessage(Player targetPlayer, Entry<Timestamp, String> currentAnnouncement) {
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
		if (restrictions().canAppointBrigadeGeneral(activePlayer, targetPlayer)) {
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
		final LegionMemberEx LM = legionMembers().getLegionMemberEx(charName);
		if (LM == null) {
			log.error(I18n.get("log.10437023e015", charName));
			return;
		}
		if (restrictions().canAppointRank(activePlayer, LM.getObjectId())) {
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
		if (restrictions().canAppointRank(activePlayer, targetPlayer.getObjectId())) {
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
		if (restrictions().canChangeSelfIntro(activePlayer, newSelfIntro)) {
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
		if (restrictions().canChangeLevel(activePlayer)) {
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
			LegionMemberEx LM = legionMembers().getLegionMemberEx(charName);
			if (LM == null || LM.getLegion() != legion) {
				return;
			}
			legionMember = getLegionMember(LM.getObjectId());
		}
		if (restrictions().canChangeNickname(legion, legionMember.getObjectId(), newNickname)) {
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
		emblems().storeCustomEmblem(activePlayer, customEmblem);
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
		emblems().storeStandardEmblem(activePlayer, legionId, emblemId, color_r, color_g, color_b, emblemType);
	}


	/**
	 * 返回军团旅长名称；找不到时返回错误占位串。
	 * Returns the brigade general name, or an error placeholder if missing.
	 *
	 * Target legion
	 * Brigade general name
	 */
	public String getBrigadeGeneralName(Legion legion) {
		for (LegionMemberEx member : legionMembers().loadLegionMemberExList(legion, null)) {
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
		for (LegionMemberEx member : legionMembers().loadLegionMemberExList(legion, null)) {
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
		if (restrictions().canOpenWarehouse(player)) {
			LegionWhUpdate(player);
			PacketSendUtility.sendPacket(player, new SM_LEGION_EDIT(0x04, player.getLegion()));// 基纳 / kinah
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
		if (restrictions().canRecreateLegion(activePlayer, legion)) {
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
			log.error(I18n.get("log.ea0f9e89569d", ex));
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
		emblems().uploadEmblemInfo(activePlayer, totalSize, color_r, color_g, color_b, emblemType);
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
		emblems().uploadEmblemData(activePlayer, size, data);
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
		emblems().sendEmblemData(player, legionEmblem, legionId, legionName);
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
		if (restrictions().canChangeAnnouncement(activePlayer.getLegionMember(), announcement)) {
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
	void storeLegionAnnouncements(Legion legion) {
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

	void addHistory(Legion legion, String text, LegionHistoryType legionHistoryType) {
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
			if (restrictions().canKickPlayer(activePlayer, charName)) {
				if (legionMembers().removeLegionMember(charName, true, activePlayer.getName())) {
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
			if (restrictions().canLeave(activePlayer)) {
				if (legionMembers().removeLegionMember(activePlayer.getName(), false, "")) {
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
	 * 清空军团与成员缓存容器。
	 * Clears the legion and legion-member caches.
	 */
	public void clearCaches() {
		allCachedLegions.clear();
		allCachedLegionMembers.clear();
	}

	/**
	 * 惰性获取成员域实现。
	 * Lazily resolves the member-domain implementation.
	 *
	 * @return 成员域 / member domain
	 */
	LegionMembers legionMembers() {
		LegionMembers current = legionMembers;
		if (current == null) {
			current = new LegionMembers(this);
			legionMembers = current;
		}
		return current;
	}

	/** 成员域实现，按需构造 / member-domain implementation, built on demand. */
	private LegionMembers legionMembers;

	/**
	 * 按角色名获取军团成员扩展信息（缓存优先，缺失则读库）。
	 * Gets extended legion member info by character name (cache first, database fallback).
	 */
	LegionMemberEx getLegionMemberEx(String playerName) {
		return legionMembers().getLegionMemberEx(playerName);
	}

	/**
	 * 载入军团成员扩展列表（排除指定成员）。
	 * Loads the extended legion member list, excluding the given member.
	 */
	public ArrayList<LegionMemberEx> loadLegionMemberExList(Legion legion, Integer objExcluded) {
		return legionMembers().loadLegionMemberExList(legion, objExcluded);
	}

	/**
	 * 玩家自行退出军团。
	 * Removes the player from the legion as itself.
	 */
	public boolean removePlayerFromLegionAsItself(Player player) {
		return legionMembers().removePlayerFromLegionAsItself(player);
	}

	/**
	 * 玩家登录时同步军团信息：成员列表、公告、解散状态与加成。
	 * On login, syncs legion info: member list, announcement, disband state and bonuses.
	 */
	public void onLogin(Player activePlayer) {
		legionMembers().onLogin(activePlayer);
	}

	/**
	 * 玩家下线时释放仓库占用、广播离线并持久化军团/成员数据。
	 * On logout, releases warehouse lock, broadcasts offline status and persists legion/group data.
	 */
	public void onLogout(Player player) {
		legionMembers().onLogout(player);
	}

	/** 世界引用，供成员域查找在线玩家 / world reference so the member domain can resolve online players. */
	World world() {
		return world;
	}

	/** 供成员域读写成员缓存 / exposes the member cache to the member domain. */
	LegionMemberContainer allCachedLegionMembers() {
		return allCachedLegionMembers;
	}

	/** 供成员域读取军团缓存 / exposes the legion cache to the member domain. */
	LegionContainer allCachedLegions() {
		return allCachedLegions;
	}

	/**
	 * 返回当前全部已缓存军团的快照列表。
	 * Returns a snapshot list of all currently cached legions.
	 *
	 * @return 缓存军团列表 / cached legions
	 */
	public List<Legion> getAllCachedLegions() {
		return allCachedLegions.getAllLegions();
	}

	LegionRestrictions restrictions() {
		LegionRestrictions current = legionRestrictions;
		if (current == null) {
			current = new LegionRestrictions(this);
			legionRestrictions = current;
		}
		return current;
	}

	/**
	 * 惰性获取徽章域实现。
	 * Lazily resolves the emblem-domain implementation.
	 *
	 * @return 徽章域 / emblem domain
	 */
	LegionEmblems emblems() {
		LegionEmblems current = legionEmblems;
		if (current == null) {
			current = new LegionEmblems(this);
			legionEmblems = current;
		}
		return current;
	}

	/** 徽章域实现，按需构造 / emblem-domain implementation, built on demand. */
	private LegionEmblems legionEmblems;

	/**
	 * 校验军团名称是否合法（匹配配置的正则）。
	 * Checks whether a legion name is valid (matches the configured pattern).
	 *
	 * @param name 军团名称 / Legion name
	 * @return 合法返回 true，否则 false / True if valid, false otherwise
	 */
	public boolean isValidName(String name) {
		return restrictions().isValidName(name);
	}

	/**
	 * 判断玩家是否在军团创建者 NPC 的交谈距离内。
	 * Checks whether the player is within the legion creator NPC's talk distance.
	 *
	 * @param player     玩家 / player
	 * @param creatorNpc 创建者 NPC / creator NPC
	 * @return 在范围内为 true / true when in range
	 */
	public static boolean isNearLegionCreator(Player player, Npc creatorNpc) {
		return LegionRestrictions.isNearLegionCreator(player, creatorNpc);
	}

	/**
	 * 查询军团（按名称）并返回匹配列表。
	 * Searches legions by type and name and answers the client.
	 */
	public void handleLegionSearch(Player player, int type, String legionName) {
		restrictions().handleLegionSearch(player, type, legionName);
	}

	/**
	 * 设置军团入团说明（仅旅长），并同步客户端与数据库。
	 * Sets the legion join description (brigade general only) and syncs client/DB.
	 */
	public void setJoinDescription(Player player, String description) {
		restrictions().setJoinDescription(player, description);
	}

	/**
	 * 设置军团入团类型（仅旅长），并同步客户端与数据库。
	 * Sets the legion join type (brigade general only) and syncs client/DB.
	 */
	public void setJoinType(Player player, int joinType) {
		restrictions().setJoinType(player, joinType);
	}

	/**
	 * 设置入团最低等级（仅旅长），并同步客户端与数据库。
	 * Sets the minimum join level (brigade general only) and syncs client/DB.
	 */
	public void setJoinMinLevel(Player player, int minLevel) {
		restrictions().setJoinMinLevel(player, minLevel);
	}

	/**
	 * 向玩家发送当前入团申请对应的军团信息包。
	 * Sends the join-request legion info packet to the player.
	 */
	public void sendLegionJoinRequestPacket(Player player, int legionId) {
		restrictions().sendLegionJoinRequestPacket(player, legionId);
	}

	/**
	 * 玩家进世界时，按 CommonData 中的申请军团 ID 重发入团申请信息包。
	 * On enter-world, resends join-request info using the legion id stored in CommonData.
	 */
	public void sendLegionJoinRequestPacketonEnterWorld(Player player) {
		restrictions().sendLegionJoinRequestPacketonEnterWorld(player);
	}

	/**
	 * 处理玩家入团申请：申请入队、直接加入或拒绝招募。
	 * Handles a player join request: apply, direct join, or reject if not recruiting.
	 */
	public void handleLegionJoinRequest(Player player, int legionId, int joinType, String joinRequestMsg) {
		restrictions().handleLegionJoinRequest(player, legionId, joinType, joinRequestMsg);
	}

	/**
	 * 取消玩家对指定军团的入团申请，并通知旅长。
	 * Cancels the player join request for a legion and notifies the brigade general.
	 */
	public void handleJoinRequestCancel(Player player, int legionId) {
		restrictions().handleJoinRequestCancel(player, legionId);
	}

	/**
	 * 玩家侧处理入团申请结果（接受则入团，拒绝则清理申请）。
	 * Applies join-request answer on the player side (join on accept, clear on deny).
	 */
	public void handleJoinRequestGetAnswer(Player player) {
		restrictions().handleJoinRequestGetAnswer(player);
	}

	/**
	 * 旅长批复入团申请：在线则即时处理，离线则写库状态。
	 * Brigade general answers a join request: handles online immediately or persists offline state.
	 */
	public void handleJoinRequestGiveAnswer(Player brigadeGeneral, int playerId, boolean accept) {
		restrictions().handleJoinRequestGiveAnswer(brigadeGeneral, playerId, accept);
	}

	/**
	 * 记录军团仓库存取物品历史（存入/取出）。
	 * Records legion warehouse item deposit/withdraw history.
	 */
	public void addWHItemHistory(Player player, int itemId, long count, IStorage sourceStorage, IStorage destStorage) {
		restrictions().addWHItemHistory(player, itemId, count, sourceStorage, destStorage);
	}
}
