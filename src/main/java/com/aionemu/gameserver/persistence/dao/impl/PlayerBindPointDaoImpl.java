package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.BindPointPosition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.PlayerBindPointDao;
import com.aionemu.gameserver.persistence.entity.PlayerBindPointPo;
import com.aionemu.gameserver.persistence.mapper.PlayerBindPointMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家绑定点数据访问实现。
 * Data access implementation for player bind points.
 * @author evilset
 */
@Slf4j
@Service
@Lazy
public class PlayerBindPointDaoImpl extends ServiceImpl<PlayerBindPointMapper, PlayerBindPointPo>
	implements PlayerBindPointDao {

	/**
	 * 加载玩家绑定点。
	 * Loads the bind point for a player.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadBindPoint(Player player) {
		try {
			PlayerBindPointPo po = mapper.selectOneByQuery(QueryWrapper.create()
				.where(PlayerBindPointPo::getPlayerId).eq(player.getObjectId()));
			if (po == null) {
				return;
			}
			BindPointPosition bind = new BindPointPosition(po.getMapId(), po.getX(), po.getY(), po.getZ(),
				po.getHeading());
			bind.setPersistentState(PersistentState.UPDATED);
			player.setBindPoint(bind);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.43ab488ee287", player.getObjectId(), " from DB"), e);
		}
	}

	/**
	 * 插入玩家绑定点。
	 * Inserts a player bind point.
	 * @param player 玩家 / player
	 * @return 是否插入成功 / whether insert succeeded
	 */
	@Override
	public boolean insertBindPoint(Player player) {
		try {
			BindPointPosition bpp = player.getBindPoint();
			int affected = mapper.replaceBindPoint(player.getObjectId(), bpp.getMapId(), bpp.getX(), bpp.getY(),
				bpp.getZ(), bpp.getHeading());
			return affected > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8f29cc17afb0", player.getObjectId(), " from DB"), e);
			return false;
		}
	}

	/**
	 * 更新玩家绑定点。
	 * Updates a player bind point.
	 * @param player 玩家 / player
	 * @return 是否更新成功 / whether update succeeded
	 */
	@Override
	public boolean updateBindPoint(Player player) {
		try {
			BindPointPosition bpp = player.getBindPoint();
			return UpdateChain.of(PlayerBindPointPo.class)
				.set(PlayerBindPointPo::getMapId, bpp.getMapId())
				.set(PlayerBindPointPo::getX, bpp.getX())
				.set(PlayerBindPointPo::getY, bpp.getY())
				.set(PlayerBindPointPo::getZ, bpp.getZ())
				.set(PlayerBindPointPo::getHeading, bpp.getHeading())
				.where(PlayerBindPointPo::getPlayerId).eq(player.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f96acb94f4a3", player.getObjectId(), " from DB"), e);
			return false;
		}
	}

	/**
	 * 按持久化状态保存玩家绑定点。
	 * Stores the player bind point according to its persistent state.
	 * @param player 玩家 / player
	 * @return 是否保存成功 / whether store succeeded
	 */
	@Override
	public boolean store(final Player player) {
		boolean insert;
		BindPointPosition bind = player.getBindPoint();

		switch (bind.getPersistentState()) {
			case NEW:
				insert = insertBindPoint(player);
				break;
			case UPDATE_REQUIRED:
				insert = updateBindPoint(player);
				break;
			default:
				return true;
		}
		bind.setPersistentState(PersistentState.UPDATED);
		return insert;
	}
}
