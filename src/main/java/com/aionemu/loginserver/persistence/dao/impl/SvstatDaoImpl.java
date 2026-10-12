package com.aionemu.loginserver.persistence.dao.impl;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.persistence.dao.SvstatDao;
import com.aionemu.loginserver.persistence.entity.SvstatPo;
import com.aionemu.loginserver.persistence.mapper.SvstatMapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服在线统计数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class SvstatDaoImpl extends ServiceImpl<SvstatMapper, SvstatPo> implements SvstatDao {

	@Override
	public void update_SvStats_Online(int server, int status, int current, int max) {
		try {
			boolean updated = UpdateChain.of(SvstatPo.class)
				.set(SvstatPo::getStatus, status)
				.set(SvstatPo::getCurrent, current)
				.set(SvstatPo::getMax, max)
				.setRaw("last_update", "NOW()")
				.where(SvstatPo::getServer).eq(server)
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
			boolean updated = UpdateChain.of(SvstatPo.class)
				.set(SvstatPo::getStatus, status)
				.set(SvstatPo::getCurrent, current)
				.setRaw("last_update", "NOW()")
				.where(SvstatPo::getServer).eq(server)
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
		SvstatPo po = new SvstatPo();
		po.setServer(server);
		po.setStatus(status);
		po.setCurrent(current);
		po.setMax(max);
		mapper.insert(po);
	}
}
