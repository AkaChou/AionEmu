package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.util.Iterator;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RENAME;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.persistence.dao.LegionDao;
import com.aionemu.gameserver.persistence.dao.OldNameDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import lombok.NonNull;

/**
 * 重命名服务，处理角色与军团的改名（含优惠券消耗与广播）。
 * Rename service that handles character and legion renames (coupon consume and broadcast).
 */
public class RenameService {

	private static volatile RenameService instance;
	@NonNull
	private final OldNameDao oldNamesDAO;
	@NonNull
	private final PlayerDao playerDAO;
	@NonNull
	private final LegionDao legionDAO;

	/**
	 * 使用指定的 DAO 依赖构造改名服务。
	 * Constructs the rename service with the given DAO dependencies.
	 *
	 * @param oldNamesDAO 旧名 DAO / old names DAO
	 * @param playerDAO   玩家 DAO / player DAO
	 * @param legionDAO   军团 DAO / legion DAO
	 */
	public RenameService(@NonNull OldNameDao oldNamesDAO, @NonNull PlayerDao playerDAO,
						 @NonNull LegionDao legionDAO) {
		this.oldNamesDAO = oldNamesDAO;
		this.playerDAO = playerDAO;
		this.legionDAO = legionDAO;
		instance = this;
	}

	public static RenameService getInstance() {
		RenameService current = instance;
		if (current == null) {
			throw new IllegalStateException("RenameService 未由 Spring 提供（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return current;
	}

	/**
	 * 使用改名券为玩家更名，并广播给所有在线玩家。
	 * Renames a player with a rename coupon and broadcasts to all online players.
	 * 玩家 / player
	 * old name
	 * new name
	 * @param item 改名券物品 objectId / rename coupon item object id
	 * @return 改名成功返回 true / true if renamed
	 */
	public static boolean renamePlayer(Player player, String oldName, String newName, int item) {
		if (!NameRestrictionService.isValidName(newName)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400151));
			return false;
		}
		if (NameRestrictionService.isForbiddenWord(newName)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400153));
			return false;
		}
		if (!PlayerService.isFreeName(newName)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400155));
			return false;
		}
		if (player.getName().equals(newName)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400153));
			return false;
		}
		if (!CustomConfig.OLD_NAMES_COUPON_DISABLED && PlayerService.isOldName(newName)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400155));
			return false;
		}
		if ((player.getInventory().getItemByObjId(item).getItemId() != 169670000
				&& player.getInventory().getItemByObjId(item).getItemId() != 169670001)
				|| (!player.getInventory().decreaseByObjectId(item, 1))) {
			AuditLogger.info(player, "Try rename youself without coupon.");
			return false;
		}
		if (!CustomConfig.OLD_NAMES_COUPON_DISABLED) {
			getInstance().oldNamesDAO.insertNames(player.getObjectId(), player.getName(), newName);
			player.getCommonData().setName(newName);
		}
		Iterator<Player> onlinePlayers = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (onlinePlayers.hasNext()) {
			Player p = onlinePlayers.next();
			if (p != null && p.getClientConnection() != null) {
				PacketSendUtility.sendPacket(p, new SM_RENAME(player.getObjectId(), oldName, newName));
			}
		}
		getInstance().playerDAO.storePlayer(player);
		return true;
	}

	/**
	 * 使用军团改名券修改玩家所属军团名称。
	 * Renames the player's legion with a legion rename coupon.
	 * @param player 玩家 / player
	 * @param name 新军团名 / new legion name
	 * @param item 改名券物品 objectId / rename coupon item object id
	 * @return 改名成功返回 true / true if renamed
	 */
	public static boolean renameLegion(Player player, String name, int item) {
		if (!player.isLegionMember()) {
			return false;
		}
		if (!GameCoreGameplayServices.legionService().isValidName(name)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400152));
			return false;
		}
		if (NameRestrictionService.isForbiddenWord(name)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400160));
			return false;
		}
		if (getInstance().legionDAO.isNameUsed(name)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400156));
			return false;
		}
		if (player.getLegion().getLegionName().equals(name)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400154));
			return false;
		}
		if ((player.getInventory().getItemByObjId(item).getItemId() != 169680000
				&& player.getInventory().getItemByObjId(item).getItemId() != 169680001)
				|| (!player.getInventory().decreaseByObjectId(item, 1))) {
			AuditLogger.info(player, "Try rename legion without coupon.");
			return false;
		}
		GameCoreGameplayServices.legionService().setLegionName(player.getLegion(), name, true);
		return true;
	}
}
