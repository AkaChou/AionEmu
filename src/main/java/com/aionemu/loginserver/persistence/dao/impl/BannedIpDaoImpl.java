package com.aionemu.loginserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.model.BannedIP;
import com.aionemu.loginserver.persistence.dao.BannedIpDao;
import com.aionemu.loginserver.persistence.entity.BannedIpPo;
import com.aionemu.loginserver.persistence.mapper.BannedIpMapper;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * IP 封禁数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class BannedIpDaoImpl extends ServiceImpl<BannedIpMapper, BannedIpPo> implements BannedIpDao {

	@Override
	public BannedIP insert(String mask) {
		return insert(mask, null);
	}

	@Override
	public BannedIP insert(String mask, Timestamp expireTime) {
		BannedIP result = new BannedIP();
		result.setMask(mask);
		result.setTimeEnd(expireTime);
		return insert(result) ? result : null;
	}

	@Override
	public boolean insert(BannedIP bannedIP) {
		try {
			BannedIpPo po = new BannedIpPo();
			po.setMask(bannedIP.getMask());
			po.setTimeEnd(bannedIP.getTimeEnd());
			int rows = mapper.insert(po);
			if (rows > 0) {
				bannedIP.setId(po.getId());
				return true;
			}
			return false;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.34a80423f0dc"), e);
			return false;
		}
	}

	@Override
	public boolean update(BannedIP bannedIP) {
		try {
			BannedIpPo po = new BannedIpPo();
			po.setId(bannedIP.getId());
			po.setMask(bannedIP.getMask());
			po.setTimeEnd(bannedIP.getTimeEnd());
			return mapper.update(po) > 0;
		} catch (DataAccessException e) {
			return false;
		}
	}

	@Override
	public boolean remove(String mask) {
		try {
			return mapper.deleteByQuery(QueryWrapper.create().eq(BannedIpPo::getMask, mask)) > 0;
		} catch (DataAccessException e) {
			return false;
		}
	}

	@Override
	public boolean remove(BannedIP bannedIP) {
		return remove(bannedIP.getMask());
	}

	@Override
	public Set<BannedIP> getAllBans() {
		Set<BannedIP> result = new HashSet<>();
		try {
			for (BannedIpPo po : mapper.selectListByQuery(QueryWrapper.create().orderBy(BannedIpPo::getId, true))) {
				BannedIP ip = new BannedIP();
				ip.setId(po.getId());
				ip.setMask(po.getMask());
				ip.setTimeEnd(po.getTimeEnd());
				result.add(ip);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
		return result;
	}

	@Override
	public void cleanExpiredBans() {
		try {
			int deleted = mapper.deleteByQuery(QueryWrapper.create()
				.where(BannedIpPo::getTimeEnd).lt(QueryMethods.currentTimestamp())
				.and(BannedIpPo::getTimeEnd).isNotNull());
			if (deleted > 0) {
				log.info(I18n.get("log.351f92913e18", deleted));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
	}
}
