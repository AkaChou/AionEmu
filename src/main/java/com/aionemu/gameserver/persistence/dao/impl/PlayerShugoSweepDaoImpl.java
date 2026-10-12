package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSweep;
import com.aionemu.gameserver.persistence.dao.PlayerShugoSweepDao;
import com.aionemu.gameserver.persistence.entity.PlayerShugoSweepPo;
import com.aionemu.gameserver.persistence.mapper.PlayerShugoSweepMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家术古扫荡（Shugo Sweep）数据访问实现。
 * Data access implementation for player Shugo Sweep data.
 * Created by Wnkrz on 24/10/2017.
 */
@Slf4j
@Service
@Lazy
public class PlayerShugoSweepDaoImpl extends ServiceImpl<PlayerShugoSweepMapper, PlayerShugoSweepPo>
	implements PlayerShugoSweepDao {

	@Override
	public void load(Player player) {
		try {
			PlayerShugoSweepPo po = mapper.selectOneByQuery(
				QueryWrapper.create().where(PlayerShugoSweepPo::getPlayerId).eq(player.getObjectId()));
			if (po != null) {
				PlayerSweep ps = new PlayerSweep(po.getSweepStep(), po.getFreeDice(), po.getBoardId());
				ps.setPersistentState(PersistentState.UPDATED);
				player.setPlayerShugoSweep(ps);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.400851d054d8", player.getObjectId()), e);
		}
	}

	@Override
	public boolean add(int playerId, int freeDice, int step, int boardId) {
		try {
			PlayerShugoSweepPo po = new PlayerShugoSweepPo();
			po.setPlayerId(playerId);
			po.setFreeDice(freeDice);
			po.setSweepStep(step);
			po.setBoardId(boardId);
			mapper.insert(po);
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.81d2b7934965", playerId), e);
			return false;
		}
	}

	@Override
	public boolean delete() {
		try {
			mapper.deleteAll();
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b01e2acd651b"), e);
			return false;
		}
	}

	@Override
	public boolean store(Player player) {
		boolean success = false;
		PlayerSweep bind = player.getPlayerShugoSweep();
		if (bind != null) {
			switch (bind.getPersistentState()) {
				case UPDATE_REQUIRED:
				case NEW:
					success = updatePlayerSweep(player);
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
	public void setShugoSweepByObjId(int obj, int freeDice, int step, int boardId) {
		try {
			UpdateChain.of(PlayerShugoSweepPo.class)
				.set(PlayerShugoSweepPo::getFreeDice, freeDice)
				.set(PlayerShugoSweepPo::getSweepStep, step)
				.set(PlayerShugoSweepPo::getBoardId, boardId)
				.where(PlayerShugoSweepPo::getPlayerId).eq(obj)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.88e530b6f3ac", obj), e);
		}
	}

	/**
	 * 更新玩家扫荡记录。
	 * Updates the player's sweep record.
	 * @param player 玩家 / player
	 * @return 是否成功 / whether succeeded
	 */
	private boolean updatePlayerSweep(Player player) {
		PlayerSweep lr = player.getPlayerShugoSweep();
		if (lr == null) {
			return false;
		}

		try {
			UpdateChain.of(PlayerShugoSweepPo.class)
				.set(PlayerShugoSweepPo::getFreeDice, lr.getFreeDice())
				.set(PlayerShugoSweepPo::getSweepStep, lr.getStep())
				.set(PlayerShugoSweepPo::getBoardId, lr.getBoardId())
				.where(PlayerShugoSweepPo::getPlayerId).eq(player.getObjectId())
				.update();
			return true;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ee22970ca710", player.getObjectId()), e);
			return false;
		}
	}
}
