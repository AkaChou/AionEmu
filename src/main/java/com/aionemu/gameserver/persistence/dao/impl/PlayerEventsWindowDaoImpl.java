package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.event_window.PlayerEventWindowEntry;
import com.aionemu.gameserver.model.event_window.PlayerEventWindowList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerEventsWindowDao;
import com.aionemu.gameserver.persistence.entity.PlayerEventsWindowPo;
import com.aionemu.gameserver.persistence.mapper.PlayerEventsWindowMapper;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家活动窗口进度数据访问实现。
 * Data access implementation for player events-window progress.
 * @author Ghostfur (Aion-Unique)
 */
@Slf4j
@Service
@Lazy
public class PlayerEventsWindowDaoImpl extends ServiceImpl<PlayerEventsWindowMapper, PlayerEventsWindowPo>
	implements PlayerEventsWindowDao {

	@Override
	public PlayerEventWindowList load(Player player) {
		List<PlayerEventWindowEntry> entries = new ArrayList<>();

		try {
			List<PlayerEventsWindowPo> rows = mapper.selectListByQuery(QueryWrapper.create()
				.eq(PlayerEventsWindowPo::getAccountId, player.getPlayerAccount().getId()));
			for (PlayerEventsWindowPo row : rows) {
				entries.add(new PlayerEventWindowEntry(row.getEventId(), row.getLastStamp(), row.getElapsed(),
					PersistentState.UPDATED));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.934e7f9ba605", player.getObjectId()), e);
		}

		return new PlayerEventWindowList(entries);
	}

	@Override
	public void insert(int accountId, int eventId, Timestamp lastStamp) {
		try {
			PlayerEventsWindowPo po = new PlayerEventsWindowPo();
			po.setAccountId(accountId);
			po.setEventId(eventId);
			po.setLastStamp(lastStamp);
			mapper.insertSelective(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.57c352a40e1f"), e);
		}
	}

	@Override
	public boolean store(int accountId, int eventId, Timestamp lastStamp, int elapsed) {
		try {
			mapper.upsert(accountId, eventId, lastStamp, elapsed);
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d5794895e11e", accountId), e);
			return false;
		}
	}

	@Override
	public void delete(int accountId, int eventId) {
		try {
			mapper.deleteByQuery(byAccountAndEvent(accountId, eventId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.10a9343136b2", accountId, eventId), e);
		}
	}

	@Override
	public Timestamp getLastStamp(int accountId, int eventId) {
		try {
			PlayerEventsWindowPo po = mapper.selectOneByQuery(byAccountAndEvent(accountId, eventId));
			if (po != null && po.getLastStamp() != null) {
				return po.getLastStamp();
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.14716135e108", accountId, eventId), e);
		}

		return new Timestamp(System.currentTimeMillis());
	}

	@Override
	public int getElapsed(int accountId, int eventId) {
		try {
			PlayerEventsWindowPo po = mapper.selectOneByQuery(byAccountAndEvent(accountId, eventId));
			return po == null || po.getElapsed() == null ? 0 : po.getElapsed();
		} catch (DataAccessException e) {
			log.debug("No elapsed time found for account {}, event {}", accountId, eventId);
		}

		return 0;
	}

	@Override
	public void updateElapsed(int accountId, int eventId, int elapsed) {
		try {
			UpdateChain.of(PlayerEventsWindowPo.class)
				.set(PlayerEventsWindowPo::getElapsed, elapsed)
				.where(PlayerEventsWindowPo::getAccountId).eq(accountId)
				.and(PlayerEventsWindowPo::getEventId).eq(eventId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.59f40fb9f03a", accountId, eventId), e);
		}
	}

	@Override
	public int getRewardRecivedCount(int accountId, int eventId) {
		try {
			PlayerEventsWindowPo po = mapper.selectOneByQuery(byAccountAndEvent(accountId, eventId));
			return po == null || po.getRewardRecivedCount() == null ? 0 : po.getRewardRecivedCount();
		} catch (DataAccessException e) {
			log.debug("No reward count found for account {}, event {}", accountId, eventId);
		}

		return 0;
	}

	@Override
	public void setRewardRecivedCount(int accountId, int eventId, int rewardRecivedCount) {
		try {
			UpdateChain.of(PlayerEventsWindowPo.class)
				.set(PlayerEventsWindowPo::getRewardRecivedCount, rewardRecivedCount)
				.set(PlayerEventsWindowPo::getElapsed, 0)
				.set(PlayerEventsWindowPo::getLastStamp, QueryMethods.now())
				.where(PlayerEventsWindowPo::getAccountId).eq(accountId)
				.and(PlayerEventsWindowPo::getEventId).eq(eventId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.90532cef7c99", accountId, eventId), e);
		}
	}

	@Override
	public List<Integer> getEventsWindow(int accountId) {
		try {
			return mapper.selectListByQuery(QueryWrapper.create()
				.eq(PlayerEventsWindowPo::getAccountId, accountId)).stream()
				.map(PlayerEventsWindowPo::getEventId)
				.toList();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.55105d71a66c", accountId), e);
			return List.of();
		}
	}

	private static QueryWrapper byAccountAndEvent(int accountId, int eventId) {
		return QueryWrapper.create()
			.eq(PlayerEventsWindowPo::getAccountId, accountId)
			.eq(PlayerEventsWindowPo::getEventId, eventId);
	}
}
