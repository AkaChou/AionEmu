package com.aionemu.loginserver.persistence.dao.impl;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.persistence.dao.SvstatsDao;
import com.aionemu.loginserver.persistence.entity.SvstatsPo;
import com.aionemu.loginserver.persistence.mapper.SvstatsMapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服在线统计数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class SvstatsDaoImpl extends ServiceImpl<SvstatsMapper, SvstatsPo> implements SvstatsDao {

	@Override
	public void update_SvStats_Online(int server, int status, int current, int max) {
		try {
			boolean updated = UpdateChain.of(SvstatsPo.class)
				.set(SvstatsPo::getStatus, status)
				.set(SvstatsPo::getCurrent, current)
				.set(SvstatsPo::getMax, max)
				.setRaw("last_update", "NOW()")
				.where(SvstatsPo::getServer).eq(server)
				.update();
			if (!updated) {
				insertServerStats(server, status, current, max);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
	}

	@Override
	public void update_SvStats_Offline(int server, int status, int current) {
		try {
			boolean updated = UpdateChain.of(SvstatsPo.class)
				.set(SvstatsPo::getStatus, status)
				.set(SvstatsPo::getCurrent, current)
				.setRaw("last_update", "NOW()")
				.where(SvstatsPo::getServer).eq(server)
				.update();
			if (!updated) {
				insertServerStats(server, status, current, 0);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
	}

	@Override
	public void update_SvStats_All_Offline(int status, int current) {
		try {
			mapper.updateAllOffline(status, current);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.13feade4c9b9"), e);
		}
	}

	private void insertServerStats(int server, int status, int current, int max) {
		SvstatsPo po = new SvstatsPo();
		po.setServer(server);
		po.setStatus(status);
		po.setCurrent(current);
		po.setMax(max);
		mapper.insert(po);
	}
}
