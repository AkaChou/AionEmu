package com.aionemu.gameserver.persistence.dao.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.PlayerVarDao;
import com.aionemu.gameserver.persistence.entity.PlayerVarPo;
import com.aionemu.gameserver.persistence.mapper.PlayerVarMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家自定义变量数据访问实现。
 * Data access implementation for player custom key-value variables.
 */
@Slf4j
@Service
@Lazy
public class PlayerVarDaoImpl extends ServiceImpl<PlayerVarMapper, PlayerVarPo> implements PlayerVarDao {

	@Override
	public Map<String, Object> load(int playerId) {
		Map<String, Object> map = new LinkedHashMap<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerVarPo::getPlayerId).eq(playerId))
				.forEach(po -> map.put(po.getParam(), po.getValue()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.eef4c3c016c8", playerId), e);
		}
		return map;
	}

	@Override
	public boolean set(int playerId, String key, Object value) {
		try {
			mapper.upsert(playerId, key, value.toString());
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.093f2900065e", playerId, key), e);
			return false;
		}
	}

	@Override
	public boolean remove(int playerId, String key) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerVarPo::getPlayerId).eq(playerId)
				.and(PlayerVarPo::getParam).eq(key));
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c3f19e683101", playerId, key), e);
			return false;
		}
	}
}
