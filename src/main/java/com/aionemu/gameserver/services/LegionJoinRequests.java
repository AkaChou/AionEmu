package com.aionemu.gameserver.services;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionHistoryType;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequestState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_EDIT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_REQUEST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_REQUEST_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_REQUEST_PLAYER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_SEARCH;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 军团入团申请流域：搜索、招募设置、申请提交/取消/批复与仓库历史记录。
 * Legion join-request flow: search, recruiting settings, apply/cancel/answer handling and warehouse history.
 *
 * <p>该类型只服务 {@link LegionRestrictions}（与 {@link LegionMembers}/{@link LegionEmblems} 同模式）：
 * 持宿主服务与权限校验器两个引用；对外仍通过 {@link LegionRestrictions}/{@link LegionService} 的
 * 原方法访问（调用链签名不变）。
 * This type only serves {@link LegionRestrictions} (same pattern as {@link LegionMembers}/
 * {@link LegionEmblems}): it holds references to the hosting service and the permission checker.
 * External callers keep using the original {@link LegionRestrictions}/{@link LegionService} methods
 * with unchanged call chains.</p>
 */
final class LegionJoinRequests {

	private final LegionService service;
	private final LegionRestrictions restrictions;

	/**
	 * 绑定宿主服务与权限校验器。
	 * Binds the hosting service and the permission checker.
	 */
	LegionJoinRequests(LegionService service, LegionRestrictions restrictions) {
		this.service = service;
		this.restrictions = restrictions;
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
	void addWHItemHistory(Player player, int itemId, long count, IStorage sourceStorage, IStorage destStorage) {
		Legion legion = player.getLegion();
		if (legion != null) {
			String description = itemId + ":" + count;
			if (sourceStorage.getStorageType() == StorageType.LEGION_WAREHOUSE) {
				service.addHistory(legion, player.getName(), LegionHistoryType.ITEM_WITHDRAW, 2,
						description);
			} else if (destStorage.getStorageType() == StorageType.LEGION_WAREHOUSE) {
				service.addHistory(legion, player.getName(), LegionHistoryType.ITEM_DEPOSIT, 2,
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
	void handleLegionSearch(Player player, int type, String legionName) {
		List<Legion> matchingLegions = new ArrayList<>();
		switch (type) {
		case 0:
			matchingLegions = service.getAllCachedLegions();
			break;
		case 1:
			for (Legion legion : service.getAllCachedLegions()) {
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
	void setJoinDescription(Player player, String description) {
		Legion legion = player.getLegion();
		if (legion == null) {
			return;
		}
		if (restrictions.canChangeLegionJoinSetting(player)) {
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
	void setJoinType(Player player, int joinType) {
		Legion legion = player.getLegion();
		if (legion == null) {
			return;
		}
		if (restrictions.canChangeLegionJoinSetting(player)) {
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
	void setJoinMinLevel(Player player, int minLevel) {
		Legion legion = player.getLegion();
		if (legion == null) {
			return;
		}
		if (restrictions.canChangeLegionJoinSetting(player)) {
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
	void sendLegionJoinRequestPacket(Player player, int legionId) {
		if (legionId <= 0) {
			PacketSendUtility.sendPacket(player, new SM_LEGION_REQUEST_INFO(0, ""));
		} else {
			Legion legion = service.getLegion(legionId);
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
	void sendLegionJoinRequestPacketonEnterWorld(Player player) {
		int legionId = player.getCommonData().getJoinRequestLegionId();
		if (legionId <= 0) {
			PacketSendUtility.sendPacket(player, new SM_LEGION_REQUEST_INFO(0, ""));
		} else {
			Legion legion = service.getLegion(legionId);
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
	void handleLegionJoinRequest(Player player, int legionId, int joinType, String joinRequestMsg) {
		Legion legion = service.getLegion(legionId);
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
			Player brigadeGeneral = service.getBrigadeGeneral(legion);
			if (brigadeGeneral != null) {
				PacketSendUtility.sendPacket(brigadeGeneral, new SM_LEGION_REQUEST_PLAYER(ljr));
			}
			break;
		case 1:
			service.directAddPlayer(legion, player);
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
	void handleJoinRequestCancel(Player player, int legionId) {
		Legion legion = service.getLegion(legionId);
		player.clearJoinRequest();
		sendLegionJoinRequestPacket(player, 0);
		legion.getJoinRequestMap().remove(player.getObjectId());
		Player bg = service.getBrigadeGeneral(legion);
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
	void handleJoinRequestGetAnswer(Player player) {
		PlayerCommonData pcd = player.getCommonData();
		switch (pcd.getJoinRequestState()) {
		case ACCEPTED:
			if (!player.isOnAStation()) {
				service.directAddPlayer(pcd.getJoinRequestLegionId(), player);
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
	void handleJoinRequestGiveAnswer(Player brigadeGeneral, int playerId, boolean accept) {
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
			legion.getJoinRequestMap().remove(playerId);
		}
		PacketSendUtility.sendPacket(brigadeGeneral, new SM_LEGION_REQUEST(playerId, accept));
		if (playerOnline) {
			player.getCommonData().setJoinRequestState(state);
			handleJoinRequestGetAnswer(player);
		}
	}

}
