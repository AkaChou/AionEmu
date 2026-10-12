package com.aionemu.loginserver.persistence.dao.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.persistence.dao.GameserversDao;
import com.aionemu.loginserver.persistence.entity.GameserversPo;
import com.aionemu.loginserver.persistence.mapper.GameserversMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服注册信息数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class GameserversDaoImpl extends ServiceImpl<GameserversMapper, GameserversPo> implements GameserversDao {

	@Override
	public Map<Byte, GameServerInfo> getAllGameServers() {
		Map<Byte, GameServerInfo> result = new HashMap<>();
		try {
			for (GameserversPo po : mapper.selectListByQuery(QueryWrapper.create().orderBy(GameserversPo::getId, true))) {
				GameServerInfo info = new GameServerInfo(po.getId().byteValue(), po.getMask(), po.getPassword());
				result.put(info.getId(), info);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
		return result;
	}
}
