package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.account.CharacterBanInfo;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService.PunishmentType;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家惩罚 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerPunishmentsDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author lord_rex, Cura, nrg
 */
@Slf4j
@Repository
public class PlayerPunishmentsDAO extends com.aionemu.gameserver.dao.PlayerPunishmentsDAO {

	/** 查询惩罚 SQL / Select punishment SQL*/
	private static final String SELECT_QUERY = "SELECT `player_id`, `start_time`, `duration`, `reason` FROM `player_punishments` WHERE `player_id`=? AND `punishment_type`=?";
	/** 更新惩罚时长 SQL / Update punishment duration SQL*/
	private static final String UPDATE_QUERY = "UPDATE `player_punishments` SET `duration`=? WHERE `player_id`=? AND `punishment_type`=?";
	/** 替换惩罚记录 SQL / Replace punishment record SQL*/
	private static final String REPLACE_QUERY = "REPLACE INTO `player_punishments` VALUES (?,?,?,?,?)";
	/** 删除惩罚 SQL / Delete punishment SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `player_punishments` WHERE `player_id`=? AND `punishment_type`=?";

	private final JdbcClient jdbcClient;

	public PlayerPunishmentsDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public PlayerPunishmentsDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载玩家惩罚数据。
	 * Loads player punishments.
	 * @param player 玩家 / player
	 * @param punishmentType 惩罚类型 / punishment type
	 */
	@Override
	public void loadPlayerPunishments(final Player player, final PunishmentType punishmentType) {
		try {
			jdbcClient.sql(SELECT_QUERY)
				.param(player.getObjectId())
				.param(punishmentType.toString())
				.query((rs, _) -> {
					if (punishmentType == PunishmentType.PRISON) {
						player.setPrisonTimer(rs.getLong("duration") * 1000);
					} else if (punishmentType == PunishmentType.GATHER) {
						player.setGatherableTimer(rs.getLong("duration") * 1000);
					}
					return null;
				})
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fa809bd49702", player.getObjectId()), e);
		}
	}

	/**
	 * 保存玩家惩罚数据。
	 * Stores player punishments.
	 * @param player 玩家 / player
	 * @param punishmentType 惩罚类型 / punishment type
	 */
	@Override
	public void storePlayerPunishments(final Player player, final PunishmentType punishmentType) {
		long duration = 0;
		if (punishmentType == PunishmentType.PRISON) {
			duration = player.getPrisonTimer() / 1000;
		} else if (punishmentType == PunishmentType.GATHER) {
			duration = (player.getGatherableTimer() - (System.currentTimeMillis() - player.getStopGatherable())) / 1000;
		}

		try {
			jdbcClient.sql(UPDATE_QUERY)
				.param(duration)
				.param(player.getObjectId())
				.param(punishmentType.toString())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4d229d45c53e", player.getObjectId()), e);
		}
	}

	/**
	 * 惩罚指定玩家。
	 * Punishes a player by id.
	 * @param playerId 玩家 ID / player id
	 * @param punishmentType 惩罚类型 / punishment type
	 * @param duration 秒数时长 / duration in seconds
	 * @param reason 原因 / reason
	 */
	@Override
	public void punishPlayer(final int playerId, final PunishmentType punishmentType, final long duration, final String reason) {
		try {
			jdbcClient.sql(REPLACE_QUERY)
				.param(playerId)
				.param(punishmentType.toString())
				.param(System.currentTimeMillis() / 1000)
				.param(duration)
				.param(reason)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5863b35bdb03", playerId), e);
		}
	}

	/**
	 * 惩罚玩家（基于当前计时器）。
	 * Punishes a player based on current timers.
	 * @param player 玩家 / player
	 * @param punishmentType 惩罚类型 / punishment type
	 * @param reason 原因 / reason
	 */
	@Override
	public void punishPlayer(final Player player, final PunishmentType punishmentType, final String reason) {
		if (punishmentType == PunishmentType.PRISON) {
			punishPlayer(player.getObjectId(), punishmentType, player.getPrisonTimer() / 1000, reason);
		} else if (punishmentType == PunishmentType.GATHER) {
			punishPlayer(player.getObjectId(), punishmentType, player.getGatherableTimer() / 1000, reason);
		}
	}

	/**
	 * 解除玩家惩罚。
	 * Unpunishes a player.
	 * @param playerId 玩家 ID / player id
	 * @param punishmentType 惩罚类型 / punishment type
	 */
	@Override
	public void unpunishPlayer(final int playerId, final PunishmentType punishmentType) {
		try {
			jdbcClient.sql(DELETE_QUERY)
				.param(playerId)
				.param(punishmentType.toString())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.327e8c487d80", playerId), e);
		}
	}

	/**
	 * 获取角色封禁信息。
	 * Gets character ban info.
	 * @param playerId 玩家 ID / player id
	 * @return 封禁信息 / ban info
	 */
	@Override
	public CharacterBanInfo getCharBanInfo(final int playerId) {
		try {
			return jdbcClient.sql(SELECT_QUERY)
				.param(playerId)
				.param(PunishmentType.CHARBAN.toString())
				.query((rs, _) -> new CharacterBanInfo(
					playerId,
					rs.getLong("start_time"),
					rs.getLong("duration"),
					rs.getString("reason")
				))
				.optional()
				.orElse(null);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c1154040f3ec", playerId), e);
			return null;
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 * @param database 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
