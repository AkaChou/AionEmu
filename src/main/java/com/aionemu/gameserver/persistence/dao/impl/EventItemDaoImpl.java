package com.aionemu.gameserver.persistence.dao.impl;

import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.event.MaxCountOfDay;
import com.aionemu.gameserver.persistence.dao.EventItemDao;
import com.aionemu.gameserver.persistence.entity.EventItemPo;
import com.aionemu.gameserver.persistence.mapper.EventItemMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 活动物品计数数据访问实现。
 * Data access implementation for event item counts.
 * @author wanke
 */
@Slf4j
@Service
@Lazy
public class EventItemDaoImpl extends ServiceImpl<EventItemMapper, EventItemPo> implements EventItemDao {

	@Override
	public void loadItems(final Player player) {
		try {
			for (EventItemPo po : mapper.selectListByQuery(
					QueryWrapper.create().where(EventItemPo::getPlayerId).eq(player.getObjectId()))) {
				player.addItemMaxCountOfDay(po.getItemId(), po.getCounts());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5aff73432829", player.getObjectId()), e);
		}
	}

	@Override
	public void storeItems(Player player) {
		deleteItems(player);

		Map<Integer, MaxCountOfDay> itemsm = player.getItemMaxThisCounts();
		if (itemsm == null || itemsm.isEmpty()) {
			return;
		}

		try {
			for (Map.Entry<Integer, MaxCountOfDay> entry : itemsm.entrySet()) {
				mapper.upsert(player.getObjectId(), entry.getKey(), entry.getValue().getThisCount());
			}
			player.clearItemMaxThisCount();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.cdff3db2bd19", player.getObjectId()), e);
		}
	}

	/**
	 * 删除指定玩家的全部活动物品记录。
	 * Deletes all event item records for a player.
	 * @param player 玩家 / player
	 */
	private void deleteItems(final Player player) {
		try {
			mapper.deleteByQuery(QueryWrapper.create().where(EventItemPo::getPlayerId).eq(player.getObjectId()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.bf40ce06a63c", player.getObjectId()), e);
		}
	}
}
