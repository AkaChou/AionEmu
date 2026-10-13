package com.aionemu.loginserver.persistence.dao.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.model.base.BannedMacEntry;
import com.aionemu.loginserver.persistence.dao.BannedMacDao;
import com.aionemu.loginserver.persistence.entity.BannedMacPo;
import com.aionemu.loginserver.persistence.mapper.BannedMacMapper;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * MAC 封禁数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class BannedMacDaoImpl extends ServiceImpl<BannedMacMapper, BannedMacPo> implements BannedMacDao {

	@Override
	public Map<String, BannedMacEntry> load() {
		Map<String, BannedMacEntry> map = new ConcurrentHashMap<>();
		try {
			for (BannedMacPo po : mapper.selectListByQuery(QueryWrapper.create())) {
				BannedMacEntry entry = new BannedMacEntry(po.getAddress(), po.getTime(), po.getDetails());
				map.put(entry.getMac(), entry);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
		return map;
	}

	@Override
	public boolean update(BannedMacEntry entry) {
		try {
			return mapper.replaceBan(entry.getMac(), entry.getTime(), entry.getDetails()) > 0;
		} catch (DataAccessException e) {
			return false;
		}
	}

	@Override
	public boolean remove(String address) {
		try {
			return mapper.deleteByQuery(QueryWrapper.create().eq(BannedMacPo::getAddress, address)) > 0;
		} catch (DataAccessException e) {
			return false;
		}
	}

	@Override
	public void cleanExpiredBans() {
		try {
			int deleted = mapper.deleteByQuery(QueryWrapper.create()
				.where(BannedMacPo::getTime).lt(QueryMethods.curDate()));
			if (deleted > 0) {
				log.info(I18n.get("log.bfee9964538a", deleted));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
	}
}
