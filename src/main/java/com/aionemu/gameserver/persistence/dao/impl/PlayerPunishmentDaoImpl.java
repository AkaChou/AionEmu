package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.account.CharacterBanInfo;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerPunishmentDao;
import com.aionemu.gameserver.persistence.entity.PlayerPunishmentPo;
import com.aionemu.gameserver.persistence.mapper.PlayerPunishmentMapper;
import com.aionemu.gameserver.services.PunishmentService.PunishmentType;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家惩罚数据访问实现。
 * Data access implementation for player punishment persistence.
 * @author lord_rex, Cura, nrg
 */
@Slf4j
@Service
@Lazy
public class PlayerPunishmentDaoImpl extends ServiceImpl<PlayerPunishmentMapper, PlayerPunishmentPo>
	implements PlayerPunishmentDao {

	/**
	 * 加载玩家惩罚数据。
	 * Loads player punishments.
	 * @param player 玩家 / player
	 * @param punishmentType 惩罚类型 / punishment type
	 */
	@Override
	public void loadPlayerPunishments(final Player player, final PunishmentType punishmentType) {
		try {
			PlayerPunishmentPo po = selectPunishment(player.getObjectId(), punishmentType);
			if (po == null) {
				return;
			}
			long duration = po.getDuration() == null ? 0L : po.getDuration();
			if (punishmentType == PunishmentType.PRISON) {
				player.setPrisonTimer(duration * 1000);
			} else if (punishmentType == PunishmentType.GATHER) {
				player.setGatherableTimer(duration * 1000);
			}
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
			UpdateChain.of(PlayerPunishmentPo.class)
				.set(PlayerPunishmentPo::getDuration, duration)
				.where(PlayerPunishmentPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerPunishmentPo::getPunishmentType).eq(punishmentType.toString())
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
	public void punishPlayer(final int playerId, final PunishmentType punishmentType, final long duration,
			final String reason) {
		try {
			mapper.replacePunishment(playerId, punishmentType.toString(), System.currentTimeMillis() / 1000, duration,
				reason);
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
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerPunishmentPo::getPlayerId).eq(playerId)
				.and(PlayerPunishmentPo::getPunishmentType).eq(punishmentType.toString()));
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
			PlayerPunishmentPo po = selectPunishment(playerId, PunishmentType.CHARBAN);
			if (po == null) {
				return null;
			}
			return new CharacterBanInfo(playerId, po.getStartTime() == null ? 0L : po.getStartTime(),
				po.getDuration() == null ? 0L : po.getDuration(), po.getReason());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c1154040f3ec", playerId), e);
			return null;
		}
	}

	private PlayerPunishmentPo selectPunishment(int playerId, PunishmentType punishmentType) {
		return mapper.selectOneByQuery(QueryWrapper.create()
			.where(PlayerPunishmentPo::getPlayerId).eq(playerId)
			.and(PlayerPunishmentPo::getPunishmentType).eq(punishmentType.toString()));
	}
}
