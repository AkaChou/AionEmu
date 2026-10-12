package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerLunaShop;
import com.aionemu.gameserver.persistence.dao.PlayerLunaShopDao;
import com.aionemu.gameserver.persistence.entity.PlayerLunaShopPo;
import com.aionemu.gameserver.persistence.mapper.PlayerLunaShopMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家月神商店（Luna Shop）数据访问实现。
 * Data access implementation for player Luna Shop data.
 * Created by wanke on 13/02/2017.
 */
@Slf4j
@Service
@Lazy
public class PlayerLunaShopDaoImpl extends ServiceImpl<PlayerLunaShopMapper, PlayerLunaShopPo>
	implements PlayerLunaShopDao {

	@Override
	public void load(Player player) {
		try {
			PlayerLunaShopPo po = mapper.selectOneByQuery(
				QueryWrapper.create().where(PlayerLunaShopPo::getPlayerId).eq(player.getObjectId()));
			if (po != null) {
				PlayerLunaShop pls = new PlayerLunaShop(po.getFreeUnder(), po.getFreeMunition(), po.getFreeChest());
				pls.setPersistentState(PersistentState.UPDATED);
				player.setPlayerLunaShop(pls);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.064f9d2d26d3", player.getObjectId()), e);
		}
	}

	@Override
	public boolean add(int playerId, boolean freeUnderpath, boolean freeFactory, boolean freeChest) {
		try {
			PlayerLunaShopPo po = new PlayerLunaShopPo();
			po.setPlayerId(playerId);
			po.setFreeUnder(freeUnderpath);
			po.setFreeMunition(freeFactory);
			po.setFreeChest(freeChest);
			mapper.insert(po);
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.44819cc1f7de", playerId), e);
			return false;
		}
	}

	@Override
	public boolean delete() {
		try {
			mapper.deleteAll();
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.4cc438cef8a8"), e);
			return false;
		}
	}

	@Override
	public boolean store(Player player) {
		boolean success = false;
		PlayerLunaShop bind = player.getPlayerLunaShop();
		if (bind != null) {
			switch (bind.getPersistentState()) {
				case UPDATE_REQUIRED:
				case NEW:
					success = updateLunaShop(player);
					log.debug("DB updated for player {}", player.getObjectId());
					break;
				default:
					success = true;
					break;
			}
			if (success) {
				bind.setPersistentState(PersistentState.UPDATED);
			}
		}
		return success;
	}

	@Override
	public void setLunaShopByObjId(int obj, boolean freeUnderpath, boolean freeFactory, boolean freeChest) {
		try {
			UpdateChain.of(PlayerLunaShopPo.class)
				.set(PlayerLunaShopPo::getFreeUnder, freeUnderpath)
				.set(PlayerLunaShopPo::getFreeMunition, freeFactory)
				.set(PlayerLunaShopPo::getFreeChest, freeChest)
				.where(PlayerLunaShopPo::getPlayerId).eq(obj)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a8a27eb03080", obj), e);
		}
	}

	/**
	 * 更新玩家月神商店记录。
	 * Updates the player's Luna Shop record.
	 * @param player 玩家 / player
	 * @return 是否成功 / whether succeeded
	 */
	private boolean updateLunaShop(Player player) {
		PlayerLunaShop lr = player.getPlayerLunaShop();
		if (lr == null) {
			return false;
		}

		try {
			UpdateChain.of(PlayerLunaShopPo.class)
				.set(PlayerLunaShopPo::getFreeUnder, lr.isFreeUnderpath())
				.set(PlayerLunaShopPo::getFreeMunition, lr.isFreeFactory())
				.set(PlayerLunaShopPo::getFreeChest, lr.isFreeChest())
				.where(PlayerLunaShopPo::getPlayerId).eq(player.getObjectId())
				.update();
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.500d6b5a68a9", player.getObjectId()), e);
			return false;
		}
	}
}
