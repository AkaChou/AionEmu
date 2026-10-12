package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.f2p.F2p;
import com.aionemu.gameserver.model.gameobjects.player.f2p.F2pAccount;
import com.aionemu.gameserver.persistence.dao.F2paccountDao;
import com.aionemu.gameserver.persistence.entity.F2paccountPo;
import com.aionemu.gameserver.persistence.mapper.F2paccountMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 免费转付费（F2P）状态数据访问实现。
 * Data access implementation for free-to-play (F2P) status.
 */
@Slf4j
@Service
@Lazy
public class F2paccountDaoImpl extends ServiceImpl<F2paccountMapper, F2paccountPo> implements F2paccountDao {

	@Override
	public void loadF2pInfo(Player player) {
		F2p f2p = new F2p(player);

		try {
			F2paccountPo po = mapper.selectOneByQuery(QueryWrapper.create()
				.eq(F2paccountPo::getPlayerId, player.getObjectId()));
			if (po != null && po.getTime() != null) {
				f2p.add(new F2pAccount(po.getTime()), false);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b700281835c0", player.getObjectId()), e);
		}

		player.setF2p(f2p);
	}

	@Override
	public boolean storeF2p(int playerId, int time) {
		try {
			return mapper.upsert(playerId, time) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.2830d63e3475", playerId), e);
			return false;
		}
	}

	@Override
	public boolean updateF2p(int playerId, int time) {
		try {
			boolean updated = UpdateChain.of(F2paccountPo.class)
				.set(F2paccountPo::getTime, time)
				.where(F2paccountPo::getPlayerId).eq(playerId)
				.update();

			if (!updated) {
				return storeF2p(playerId, time);
			}

			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4295e27fcc73", playerId), e);
			return false;
		}
	}

	@Override
	public boolean deleteF2p(int playerId) {
		try {
			return mapper.deleteByQuery(QueryWrapper.create().eq(F2paccountPo::getPlayerId, playerId)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.34f50ac7e4cb", playerId), e);
			return false;
		}
	}
}
