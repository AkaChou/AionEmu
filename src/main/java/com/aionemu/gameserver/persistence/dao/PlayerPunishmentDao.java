package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.account.CharacterBanInfo;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerPunishmentPo;
import com.aionemu.gameserver.services.PunishmentService.PunishmentType;
import com.mybatisflex.core.service.IService;

/**
 * 玩家惩罚数据访问层。
 * Data access layer for player punishment persistence.
 * @author lord_rex
 */
public interface PlayerPunishmentDao extends IService<PlayerPunishmentPo> {

	/**
	 * 加载玩家指定类型的惩罚数据。
	 * Loads punishments of the given type for the player.
	 * @param player 玩家 / player
	 * @param punishmentType 惩罚类型 / punishment type
	 */
	void loadPlayerPunishments(Player player, PunishmentType punishmentType);

	/**
	 * 保存玩家指定类型的惩罚数据。
	 * Stores punishments of the given type for the player.
	 * @param player 玩家 / player
	 * @param punishmentType 惩罚类型 / punishment type
	 */
	void storePlayerPunishments(Player player, PunishmentType punishmentType);

	/**
	 * 按玩家 ID 施加惩罚。
	 * Applies a punishment to the player by id.
	 * @param playerId 玩家对象 ID / player object id
	 * @param punishmentType 惩罚类型 / punishment type
	 * @param expireTime 过期时间戳 / expiration timestamp
	 * @param reason 惩罚原因 / punishment reason
	 */
	void punishPlayer(int playerId, PunishmentType punishmentType, long expireTime, String reason);

	/**
	 * 对在线玩家施加惩罚。
	 * Applies a punishment to the online player.
	 * @param player 玩家 / player
	 * @param punishmentType 惩罚类型 / punishment type
	 * @param reason 惩罚原因 / punishment reason
	 */
	void punishPlayer(Player player, PunishmentType punishmentType, String reason);

	/**
	 * 解除玩家指定类型的惩罚。
	 * Removes a punishment of the given type from the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param punishmentType 惩罚类型 / punishment type
	 */
	void unpunishPlayer(int playerId, PunishmentType punishmentType);

	/**
	 * 查询角色封禁信息。
	 * Returns character ban info for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 角色封禁信息 / character ban info
	 */
	CharacterBanInfo getCharBanInfo(int playerId);
}
