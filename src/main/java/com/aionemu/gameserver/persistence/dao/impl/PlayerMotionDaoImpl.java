package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.model.gameobjects.player.motion.MotionList;
import com.aionemu.gameserver.persistence.dao.PlayerMotionDao;
import com.aionemu.gameserver.persistence.entity.PlayerMotionPo;
import com.aionemu.gameserver.persistence.mapper.PlayerMotionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家动作数据访问实现。
 * Data access implementation for player motions.
 * @author MrPoke
 */
@Slf4j
@Service
@Lazy
public class PlayerMotionDaoImpl extends ServiceImpl<PlayerMotionMapper, PlayerMotionPo> implements PlayerMotionDao {

	/**
	 * 加载玩家动作列表并设置到玩家对象。
	 * Loads the player's motion list and assigns it to the player.
	 * @param player 玩家 / player
	 */
	@Override
	public void loadMotionList(Player player) {
		MotionList motions = new MotionList(player);
		List<Motion> loadedMotions = loadMotions(player.getObjectId());

		if (loadedMotions != null) {
			for (Motion motion : loadedMotions) {
				motions.add(motion, false);
			}
		}
		player.setMotions(motions);
	}

	/**
	 * 按玩家 ID 加载动作列表。
	 * Loads the motion list by player id.
	 * @param playerId 玩家 ID / player id
	 * @return 动作列表 / motion list
	 */
	@Override
	public List<Motion> loadMotions(Integer playerId) {
		List<Motion> motions = new ArrayList<>();

		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerMotionPo::getPlayerId).eq(playerId))
				.forEach(po -> motions.add(new Motion(po.getMotionId(), po.getTime(), po.getActive())));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.f1ef66964c92", playerId), e);
		}
		return motions;
	}

	/**
	 * 保存动作。
	 * Stores a motion.
	 * @param objectId 玩家对象 ID / player object id
	 * @param motion 动作 / motion
	 */
	@Override
	public void storeMotion(int objectId, Motion motion) {
		try {
			PlayerMotionPo po = new PlayerMotionPo();
			po.setPlayerId(objectId);
			po.setMotionId(motion.getId());
			po.setActive(motion.isActive());
			po.setTime(motion.getExpireTime());
			mapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.ee15090dda12", objectId), e);
		}
	}

	/**
	 * 删除动作。
	 * Deletes a motion.
	 * @param objectId 玩家对象 ID / player object id
	 * @param motionId 动作 ID / motion id
	 */
	@Override
	public void deleteMotion(int objectId, int motionId) {
		try {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerMotionPo::getPlayerId).eq(objectId)
				.and(PlayerMotionPo::getMotionId).eq(motionId));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.1bf60097aea6", objectId), e);
		}
	}

	/**
	 * 更新动作激活状态。
	 * Updates the motion active state.
	 * @param objectId 玩家对象 ID / player object id
	 * @param motion 动作 / motion
	 */
	@Override
	public void updateMotion(int objectId, Motion motion) {
		try {
			UpdateChain.of(PlayerMotionPo.class)
				.set(PlayerMotionPo::getActive, motion.isActive())
				.where(PlayerMotionPo::getPlayerId).eq(objectId)
				.and(PlayerMotionPo::getMotionId).eq(motion.getId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.91b2b22576c2", objectId), e);
		}
	}
}
