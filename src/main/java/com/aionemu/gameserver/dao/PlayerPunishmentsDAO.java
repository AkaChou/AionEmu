package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.account.CharacterBanInfo;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService.PunishmentType;

/**
 * 玩家惩罚数据访问抽象层。
 * DAO for player punishment persistence.
 *
 * @author lord_rex
 */
public abstract class PlayerPunishmentsDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerPunishmentsDAO.class.getName();
	}

	/**
	 * 加载玩家指定类型的惩罚数据。
	 * Loads punishments of the given type for the player.
	 *
	 * 玩家 / player
	 * punishment type
	 */
	public abstract void loadPlayerPunishments(final Player player, final PunishmentType punishmentType);

	/**
	 * 保存玩家指定类型的惩罚数据。
	 * Stores punishments of the given type for the player.
	 *
	 * 玩家 / player
	 * punishment type
	 */
	public abstract void storePlayerPunishments(final Player player, final PunishmentType punishmentType);

	/**
	 * 按玩家 ID 施加惩罚。
	 * Applies a punishment to the player by id.
	 *
	 * player object id
	 * punishment type
	 * @param expireTime 过期时间戳 / expiration timestamp
	 * punishment reason
	 */
	public abstract void punishPlayer(final int playerId, final PunishmentType punishmentType, final long expireTime,
			final String reason);

	/**
	 * 对在线玩家施加惩罚。
	 * Applies a punishment to the online player.
	 *
	 * 玩家 / player
	 * punishment type
	 * punishment reason
	 */
	public abstract void punishPlayer(final Player player, final PunishmentType punishmentType, final String reason);

	/**
	 * 解除玩家指定类型的惩罚。
	 * Removes a punishment of the given type from the player.
	 *
	 * player object id
	 * punishment type
	 */
	public abstract void unpunishPlayer(final int playerId, final PunishmentType punishmentType);

	/**
	 * 查询角色封禁信息。
	 * Returns character ban info for the player.
	 *
	 * player object id
	 * @return 角色封禁信息 / character ban info
	 */
	public abstract CharacterBanInfo getCharBanInfo(final int playerId);
}
