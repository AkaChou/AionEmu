package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeEntry;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerWardrobeDao;
import com.aionemu.gameserver.persistence.entity.PlayerWardrobePo;
import com.aionemu.gameserver.persistence.mapper.PlayerWardrobeMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家衣柜（时装）数据访问实现。
 * Data access implementation for player wardrobe (appearance skin) persistence.
 * @author Ranastic
 */
@Slf4j
@Service
@Lazy
public class PlayerWardrobeDaoImpl extends ServiceImpl<PlayerWardrobeMapper, PlayerWardrobePo>
	implements PlayerWardrobeDao {

	@Override
	public PlayerWardrobeList load(Player player) {
		List<PlayerWardrobeEntry> entries = new ArrayList<>();

		try {
			List<PlayerWardrobePo> rows = mapper.selectListByQuery(QueryWrapper.create()
				.eq(PlayerWardrobePo::getPlayerId, player.getObjectId()));
			for (PlayerWardrobePo row : rows) {
				entries.add(new PlayerWardrobeEntry(row.getItemId(), row.getSlot(), row.getReskinCount(),
					PersistentState.UPDATED));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0c81ac00324b", player.getObjectId()), e);
		}

		return new PlayerWardrobeList(entries);
	}

	@Override
	public boolean store(int objectId, int itemId, int slot, int reskin) {
		try {
			mapper.upsert(objectId, itemId, slot, reskin);
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7fed95e25235", objectId), e);
			return false;
		}
	}

	@Override
	public boolean delete(int objectId, int itemId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.eq(PlayerWardrobePo::getPlayerId, objectId)
				.eq(PlayerWardrobePo::getItemId, itemId));
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.06e3e78e5e3b", objectId), e);
			return false;
		}
	}

	@Override
	public int getItemSize(int playerObjId) {
		try {
			return (int) mapper.selectCountByQuery(QueryWrapper.create()
				.eq(PlayerWardrobePo::getPlayerId, playerObjId));
		} catch (DataAccessException e) {
			log.debug("Could not get item size for player {}", playerObjId, e);
			return 0;
		}
	}

	@Override
	public int getWardrobeItemBySlot(int playerObjId, int slot) {
		try {
			PlayerWardrobePo po = mapper.selectOneByQuery(QueryWrapper.create()
				.eq(PlayerWardrobePo::getPlayerId, playerObjId)
				.eq(PlayerWardrobePo::getSlot, slot));
			return po == null || po.getItemId() == null ? 0 : po.getItemId();
		} catch (DataAccessException e) {
			log.debug("No wardrobe item found for player {}, slot {}", playerObjId, slot);
			return 0;
		}
	}

	@Override
	public int getReskinCountBySlot(int playerObjId, int slot) {
		try {
			PlayerWardrobePo po = mapper.selectOneByQuery(QueryWrapper.create()
				.eq(PlayerWardrobePo::getPlayerId, playerObjId)
				.eq(PlayerWardrobePo::getSlot, slot));
			return po == null || po.getReskinCount() == null ? 0 : po.getReskinCount();
		} catch (DataAccessException e) {
			log.debug("No reskin count found for player {}, slot {}", playerObjId, slot);
			return 0;
		}
	}

	@Override
	public void setReskinCountBySlot(int playerObjId, int slot, int reskinCount) {
		try {
			UpdateChain.of(PlayerWardrobePo.class)
				.set(PlayerWardrobePo::getReskinCount, reskinCount)
				.where(PlayerWardrobePo::getPlayerId).eq(playerObjId)
				.and(PlayerWardrobePo::getSlot).eq(slot)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.5acbf781683c", playerObjId, slot), e);
		}
	}
}
