package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.ServerVariableDao;
import com.aionemu.gameserver.persistence.entity.ServerVariablePo;
import com.aionemu.gameserver.persistence.mapper.ServerVariableMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 服务器变量数据访问实现。
 * Data access implementation for server-wide variables.
 */
@Slf4j
@Service
@Lazy
public class ServerVariableDaoImpl extends ServiceImpl<ServerVariableMapper, ServerVariablePo>
	implements ServerVariableDao {

	@Override
	public int load(String var) {
		try {
			ServerVariablePo po = mapper.selectOneByQuery(
				QueryWrapper.create().where(ServerVariablePo::getKey).eq(var));
			String value = po == null ? null : po.getValue();
			if (value != null) {
				return Integer.parseInt(value);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.dc2937405e21"), e);
		}
		return 0;
	}

	@Override
	public boolean store(String var, int value) {
		try {
			return mapper.replace(var, String.valueOf(value)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.38420527c181"), e);
			return false;
		}
	}
}
