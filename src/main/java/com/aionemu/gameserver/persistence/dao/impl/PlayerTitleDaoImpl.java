package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.persistence.dao.PlayerTitleDao;
import com.aionemu.gameserver.persistence.entity.PlayerTitlePo;
import com.aionemu.gameserver.persistence.mapper.PlayerTitleMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家称号列表数据访问实现。
 * Data access implementation for player title list persistence.
 *
 * @author xavier
 */
@Slf4j
@Service
@Lazy
public class PlayerTitleDaoImpl extends ServiceImpl<PlayerTitleMapper, PlayerTitlePo> implements PlayerTitleDao {

	@Override
	public TitleList loadTitleList(int playerId) {
		TitleList titleList = new TitleList();
		int currentTime = (int) (System.currentTimeMillis() / 1000);

		try {
			mapper.selectListByQuery(QueryWrapper.create()
				.where(PlayerTitlePo::getPlayerId).eq(playerId)
				.orderBy(PlayerTitlePo::getTitleId, true))
				.forEach(po -> {
					int id = po.getTitleId();
					int remaining = po.getRemaining();

					// 跳过过期头衔 / Skip expired titles
					if (remaining > 0 && remaining <= currentTime) {
						removeTitle(playerId, id);
						return;
					}

					titleList.addEntry(id, remaining);
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7f727baa8f02", playerId), e);
		}

		return titleList;
	}

	@Override
	public void storeInTransaction(Connection connection, int playerId, int titleId, int remaining) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement(PlayerTitleMapper.INSERT_QUERY_JDBC)) {
			stmt.setInt(1, playerId);
			stmt.setInt(2, titleId);
			stmt.setInt(3, remaining);
			stmt.executeUpdate();
		}
	}

	@Override
	public void storeTitles(Player player, Title entry) {
		try {
			mapper.upsert(player.getObjectId(), entry.id(), entry.getExpireTime());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8893f821f5fc", player.getObjectId(), entry.id()), e);
		}
	}

	@Override
	public void removeTitle(int playerId, int titleId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerTitlePo::getPlayerId).eq(playerId)
				.and(PlayerTitlePo::getTitleId).eq(titleId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4ff4bda7010a", playerId, titleId), e);
		}
	}
}
