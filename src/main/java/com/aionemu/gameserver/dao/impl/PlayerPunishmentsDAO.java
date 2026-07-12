package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.account.CharacterBanInfo;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService.PunishmentType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 玩家惩罚 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerPunishmentsDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author lord_rex, Cura, nrg
 */
@Slf4j
public class PlayerPunishmentsDAO extends com.aionemu.gameserver.dao.PlayerPunishmentsDAO {


	/** 查询惩罚 SQL / Select punishment SQL*/
	private static final String SELECT_QUERY = "SELECT `player_id`, `start_time`, `duration`, `reason` FROM `player_punishments` WHERE `player_id`=? AND `punishment_type`=?";
	/** 更新惩罚时长 SQL / Update punishment duration SQL*/
	private static final String UPDATE_QUERY = "UPDATE `player_punishments` SET `duration`=? WHERE `player_id`=? AND `punishment_type`=?";
	/** 替换惩罚记录 SQL / Replace punishment record SQL*/
	private static final String REPLACE_QUERY = "REPLACE INTO `player_punishments` VALUES (?,?,?,?,?)";
	/** 删除惩罚 SQL / Delete punishment SQL*/
	private static final String DELETE_QUERY = "DELETE FROM `player_punishments` WHERE `player_id`=? AND `punishment_type`=?";

	/**
	 * 加载玩家惩罚数据。
	 * Loads player punishments.
	 *
	 * 玩家 / player
	 * punishment type
	 */
	@Override
	public void loadPlayerPunishments(final Player player, final PunishmentType punishmentType) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_QUERY)) {

			ps.setInt(1, player.getObjectId());
			ps.setString(2, punishmentType.toString());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (punishmentType == PunishmentType.PRISON) {
						player.setPrisonTimer(rs.getLong("duration") * 1000);
					} else if (punishmentType == PunishmentType.GATHER) {
						player.setGatherableTimer(rs.getLong("duration") * 1000);
					}
				}
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.fa809bd49702", player.getObjectId(), e));
		}
	}

	/**
	 * 保存玩家惩罚数据。
	 * Stores player punishments.
	 *
	 * 玩家 / player
	 * punishment type
	 */
	@Override
	public void storePlayerPunishments(final Player player, final PunishmentType punishmentType) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(UPDATE_QUERY)) {

			if (punishmentType == PunishmentType.PRISON) {
				ps.setLong(1, player.getPrisonTimer() / 1000);
			} else if (punishmentType == PunishmentType.GATHER) {
				ps.setLong(1, (player.getGatherableTimer() - (System.currentTimeMillis() - player.getStopGatherable())) / 1000);
			}
			ps.setInt(2, player.getObjectId());
			ps.setString(3, punishmentType.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.4d229d45c53e", player.getObjectId(), e));
		}
	}

	/**
	 * 惩罚指定玩家。
	 * Punishes a player by id.
	 *
	 * player id
	 * punishment type
	 * duration in seconds
	 * reason
	 */
	@Override
	public void punishPlayer(final int playerId, final PunishmentType punishmentType, final long duration, final String reason) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(REPLACE_QUERY)) {

			ps.setInt(1, playerId);
			ps.setString(2, punishmentType.toString());
			ps.setLong(3, System.currentTimeMillis() / 1000);
			ps.setLong(4, duration);
			ps.setString(5, reason);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.5863b35bdb03", playerId, e));
		}
	}

	/**
	 * 惩罚玩家（基于当前计时器）。
	 * Punishes a player based on current timers.
	 *
	 * 玩家 / player
	 * punishment type
	 * reason
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
	 *
	 * player id
	 * punishment type
	 */
	@Override
	public void unpunishPlayer(final int playerId, final PunishmentType punishmentType) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {

			ps.setInt(1, playerId);
			ps.setString(2, punishmentType.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.327e8c487d80", playerId, e));
		}
	}

	/**
	 * 获取角色封禁信息。
	 * Gets character ban info.
	 *
	 * player id
	 * ban info
	 */
	@Override
	public CharacterBanInfo getCharBanInfo(final int playerId) {
		final CharacterBanInfo[] charBan = new CharacterBanInfo[1];

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_QUERY)) {

			ps.setInt(1, playerId);
			ps.setString(2, PunishmentType.CHARBAN.toString());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					charBan[0] = new CharacterBanInfo(playerId, rs.getLong("start_time"), rs.getLong("duration"), rs.getString("reason"));
				}
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.c1154040f3ec", playerId, e));
		}
		return charBan[0];
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param s 数据库名 / database name
	 * @param i 主版本 / major version
	 * @param i1 次版本 / minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String s, int i, int i1) {
		return DAOUtils.supports(s, i, i1);
	}
}
