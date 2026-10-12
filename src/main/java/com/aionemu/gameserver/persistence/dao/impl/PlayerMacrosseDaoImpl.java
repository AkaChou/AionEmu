package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.MacroList;
import com.aionemu.gameserver.persistence.dao.PlayerMacrosseDao;
import com.aionemu.gameserver.persistence.entity.PlayerMacrossePo;
import com.aionemu.gameserver.persistence.mapper.PlayerMacrosseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家宏数据访问实现。
 * Data access implementation for player macros (Macrosses).
 * @author Aquanox
 */
@Slf4j
@Service
@Lazy
public class PlayerMacrosseDaoImpl extends ServiceImpl<PlayerMacrosseMapper, PlayerMacrossePo>
	implements PlayerMacrosseDao {

	/**
	 * 新增玩家宏。
	 * Adds a player macro.
	 * @param playerId 玩家 ID / player id
	 * @param macroPosition 宏槽位 / macro slot
	 * @param macro 宏文本 / macro text
	 */
	@Override
	public void addMacro(final int playerId, final int macroPosition, final String macro) {
		try {
			log.debug("[DAO: PlayerMacrossesDAO] storing macro {} {}", playerId, macroPosition);
			PlayerMacrossePo po = new PlayerMacrossePo();
			po.setPlayerId(playerId);
			po.setOrder(macroPosition);
			po.setMacro(macro);
			mapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6fb6dc66106b", playerId, macroPosition), e);
		}
	}

	/**
	 * 更新玩家宏。
	 * Updates a player macro.
	 * @param playerId 玩家 ID / player id
	 * @param macroPosition 宏槽位 / macro slot
	 * @param macro 宏文本 / macro text
	 */
	@Override
	public void updateMacro(final int playerId, final int macroPosition, final String macro) {
		try {
			log.debug("[DAO: PlayerMacrossesDAO] updating macro {} {}", playerId, macroPosition);
			UpdateChain.of(PlayerMacrossePo.class)
				.set(PlayerMacrossePo::getMacro, macro)
				.where(PlayerMacrossePo::getPlayerId).eq(playerId)
				.and(PlayerMacrossePo::getOrder).eq(macroPosition)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a1520e1b6fea", playerId, macroPosition), e);
		}
	}

	/**
	 * 删除玩家宏。
	 * Deletes a player macro.
	 * @param playerId 玩家 ID / player id
	 * @param macroPosition 宏槽位 / macro slot
	 */
	@Override
	public void deleteMacro(final int playerId, final int macroPosition) {
		try {
			log.debug("[DAO: PlayerMacrossesDAO] removing macro {} {}", playerId, macroPosition);
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerMacrossePo::getPlayerId).eq(playerId)
				.and(PlayerMacrossePo::getOrder).eq(macroPosition));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7e1652b6543b", playerId, macroPosition), e);
		}
	}

	/**
	 * 恢复玩家全部宏。
	 * Restores all macros for a player.
	 * @param playerId 玩家 ID / player id
	 * @return 宏列表 / macro list
	 */
	@Override
	public MacroList restoreMacrosses(final int playerId) {
		final Map<Integer, String> macrosses = new HashMap<>();

		try {
			log.debug("[DAO: PlayerMacrossesDAO] loading macroses for playerId: {}", playerId);
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerMacrossePo::getPlayerId).eq(playerId))
				.forEach(po -> macrosses.put(po.getOrder(), po.getMacro()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.11ce99709119", playerId, " from DB"), e);
		}
		return new MacroList(macrosses);
	}
}
