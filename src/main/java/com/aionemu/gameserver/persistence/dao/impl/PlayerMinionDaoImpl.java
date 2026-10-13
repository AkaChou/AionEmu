package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;
import com.aionemu.gameserver.persistence.dao.PlayerMinionDao;
import com.aionemu.gameserver.persistence.entity.PlayerMinionPo;
import com.aionemu.gameserver.persistence.mapper.PlayerMinionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家随从（Minion）数据访问实现。
 * Data access implementation for player minions.
 */
@Slf4j
@Service
@Lazy
public class PlayerMinionDaoImpl extends ServiceImpl<PlayerMinionMapper, PlayerMinionPo> implements PlayerMinionDao {

	/**
	 * 插入新的玩家随从记录。
	 * Inserts a new player minion record.
	 *
	 * @param minionCommonData 随从公共数据 / minion common data
	 */
	@Override
	public boolean insertPlayerMinion(MinionCommonData minionCommonData) {
		try {
			return mapper.insertMinion(toPo(minionCommonData)) == 1;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.2527877e3b8c", minionCommonData.getMinionId(), minionCommonData.getName()), e);
			return false;
		}
	}

	/**
	 * 移除玩家的指定随从。
	 * Removes the specified minion for the player.
	 *
	 * @param player         玩家 / player
	 * @param minionObjectId 仆从对象 ID / minion object id
	 */
	@Override
	public boolean removePlayerMinion(Player player, int minionObjectId) {
		try {
			return mapper.deleteByQuery(minionKey(player.getObjectId(), minionObjectId)) == 1;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.85f2880b076f", minionObjectId), e);
			return false;
		}
	}

	/**
	 * 加载玩家的全部随从数据。
	 * Loads all minions belonging to the player.
	 *
	 * @param player 玩家 / player
	 * @return 仆从列表 / minion list
	 */
	@Override
	public List<MinionCommonData> getPlayerMinions(Player player) {
		List<MinionCommonData> minions;

		try {
			minions = new ArrayList<>();
			for (PlayerMinionPo po : mapper.selectListByQuery(
					QueryWrapper.create().where(PlayerMinionPo::getPlayerId).eq(player.getObjectId()))) {
				minions.add(toMinionCommonData(po, player));
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6b0198f98c6b", player.getObjectId()), e);
			return null;
		}

		return minions;
	}

	/**
	 * 更新随从名称。
	 * Updates the minion name.
	 *
	 * @param minionCommonData 随从公共数据 / minion common data
	 */
	@Override
	public boolean updateMinionName(MinionCommonData minionCommonData) {
		try {
			return UpdateChain.of(PlayerMinionPo.class)
				.set(PlayerMinionPo::getName, minionCommonData.getName())
				.where(PlayerMinionPo::getPlayerId).eq(minionCommonData.getMasterObjectId())
				.and(PlayerMinionPo::getObjectId).eq(minionCommonData.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d5697671e606", minionCommonData.getMinionId()), e);
			return false;
		}
	}

	/**
	 * 更新玩家随从的成长点。
	 * Updates the growth points of a player minion.
	 *
	 * @param player           玩家 / player
	 * @param minionCommonData 随从公共数据 / minion common data
	 */
	@Override
	@Transactional
	public boolean updateGrowthAndRemoveMaterials(Player player, MinionCommonData minionCommonData,
			List<Integer> materialObjectIds) {
		try {
			boolean updated = UpdateChain.of(PlayerMinionPo.class)
				.set(PlayerMinionPo::getGrowthpoints, minionCommonData.getMinionGrowthPoint())
				.where(PlayerMinionPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerMinionPo::getObjectId).eq(minionCommonData.getObjectId())
				.update();
			if (!updated) {
				markRollbackOnly();
				return false;
			}
			if (materialObjectIds != null) {
				for (int materialObjectId : materialObjectIds) {
					if (mapper.deleteByQuery(minionKey(player.getObjectId(), materialObjectId)) != 1) {
						markRollbackOnly();
						return false;
					}
				}
			}
			return true;
		} catch (RuntimeException e) {
			log.error(I18n.get("log.d7a903023f02", minionCommonData.getMinionId()), e);
			markRollbackOnly();
			return false;
		}
	}

	@Override
	@Transactional
	public boolean replacePlayerMinions(MinionCommonData replacement, List<Integer> materialObjectIds) {
		try {
			if (mapper.insertMinion(toPo(replacement)) != 1) {
				markRollbackOnly();
				return false;
			}
			if (materialObjectIds != null) {
				for (int materialObjectId : materialObjectIds) {
					if (mapper.deleteByQuery(minionKey(replacement.getMasterObjectId(), materialObjectId)) != 1) {
						markRollbackOnly();
						return false;
					}
				}
			}
			return true;
		} catch (RuntimeException e) {
			log.error(I18n.get("log.2527877e3b8c", replacement.getMinionId(), replacement.getName()), e);
			markRollbackOnly();
			return false;
		}
	}

	/**
	 * 执行随从进化（更新 minion_id 与 level，重置成长点）。
	 * Evolves a minion (updates minion_id and level, resets growth points).
	 *
	 * @param player           玩家 / player
	 * @param minionCommonData 随从公共数据 / minion common data
	 */
	@Override
	public boolean evolutionMinion(Player player, MinionCommonData minionCommonData) {
		try {
			return UpdateChain.of(PlayerMinionPo.class)
				.set(PlayerMinionPo::getMinionId, minionCommonData.getMinionId())
				.set(PlayerMinionPo::getGrowthpoints, 0)
				.set(PlayerMinionPo::getLevel, minionCommonData.getMinionLevel())
				.where(PlayerMinionPo::getPlayerId).eq(minionCommonData.getMasterObjectId())
				.and(PlayerMinionPo::getObjectId).eq(minionCommonData.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.188874aa8e51", minionCommonData.getMinionId()), e);
			return false;
		}
	}

	/**
	 * 锁定或解锁指定随从。
	 * Locks or unlocks the specified minion.
	 *
	 * @param player      玩家 / player
	 * @param minionObjId 仆从对象 ID / minion object id
	 * @param isLocked    锁定标记 / lock flag
	 */
	@Override
	public boolean lockMinions(Player player, int minionObjId, int isLocked) {
		try {
			return UpdateChain.of(PlayerMinionPo.class)
				.set(PlayerMinionPo::getLocked, isLocked)
				.where(PlayerMinionPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerMinionPo::getObjectId).eq(minionObjId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.65bb4c6328f8", minionObjId), e);
			return false;
		}
	}

	/**
	 * 保存随从的增益（doping）背包内容。
	 * Saves the minion doping bag contents.
	 *
	 * @param player           玩家 / player
	 * @param minionCommonData 随从公共数据 / minion common data
	 * @param bag              增益背包 / doping bag
	 */
	@Override
	public boolean saveDopingBag(Player player, MinionCommonData minionCommonData, MinionDopingBag bag) {
		if (bag == null) {
			log.warn(I18n.get("log.342c6a28af85", minionCommonData.getObjectId()));
			return false;
		}

		try {
			StringBuilder itemIds = new StringBuilder();

			itemIds.append(bag.getFoodItem() > 0 ? bag.getFoodItem() : "0");
			itemIds.append(",");
			itemIds.append(bag.getDrinkItem() > 0 ? bag.getDrinkItem() : "0");

			for (int itemId : bag.getScrollsUsed()) {
				itemIds.append(",").append(itemId > 0 ? itemId : "0");
			}

			return UpdateChain.of(PlayerMinionPo.class)
				.set(PlayerMinionPo::getBuffBag, itemIds.toString())
				.where(PlayerMinionPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerMinionPo::getObjectId).eq(minionCommonData.getObjectId())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0c8cc68553ee", minionCommonData.getObjectId()), e);
			return false;
		}
	}

	@Override
	public int[] getUsedIDs() {
		try {
			List<PlayerMinionPo> rows = mapper.selectListByQuery(QueryWrapper.create().select(PlayerMinionPo::getObjectId));
			return rows.stream().mapToInt(PlayerMinionPo::getObjectId).toArray();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.6b0198f98c6b", 0), e);
			return new int[0];
		}
	}

	/**
	 * 从数据库读取并写回随从生日时间戳。
	 * Loads the minion birthday timestamp from the database into the model.
	 *
	 * @param minionCommonData 随从公共数据 / minion common data
	 */
	@Override
	public void saveBirthday(MinionCommonData minionCommonData) {
		try {
			PlayerMinionPo po = mapper.selectOneByQuery(QueryWrapper.create()
				.select(PlayerMinionPo::getBirthday)
				.where(PlayerMinionPo::getPlayerId).eq(minionCommonData.getMasterObjectId())
				.and(PlayerMinionPo::getObjectId).eq(minionCommonData.getObjectId()));

			if (po != null && po.getBirthday() != null) {
				minionCommonData.setBirthday(po.getBirthday());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.8896cfb9d7f2", minionCommonData.getMasterObjectId()), e);
		}
	}

	/**
	 * 构造以玩家与随从对象 ID 为条件的查询包装器。
	 * Builds a query wrapper keyed by player and minion object id.
	 * @param playerId 玩家对象 ID / player object id
	 * @param minionObjectId 随从对象 ID / minion object id
	 * @return 查询包装器 / query wrapper
	 */
	private static QueryWrapper minionKey(int playerId, int minionObjectId) {
		return QueryWrapper.create()
			.where(PlayerMinionPo::getPlayerId).eq(playerId)
			.and(PlayerMinionPo::getObjectId).eq(minionObjectId);
	}

	/**
	 * 在当前声明式事务中标记回滚，保留原编程式事务的失败语义。
	 * Marks the current declarative transaction as rollback-only, keeping the original programmatic-transaction
	 * failure semantics.
	 */
	private static void markRollbackOnly() {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		}
	}

	/**
	 * 将随从模型转换为持久化实体（仅覆盖库内允许写入的列）。
	 * Converts the minion model into a persistence entity (only the columns the database accepts).
	 * @param minionCommonData 随从公共数据 / minion common data
	 * @return 随从持久化实体 / minion persistence entity
	 */
	private static PlayerMinionPo toPo(MinionCommonData minionCommonData) {
		PlayerMinionPo po = new PlayerMinionPo();
		po.setPlayerId(minionCommonData.getMasterObjectId());
		po.setObjectId(minionCommonData.getObjectId());
		po.setMinionId(minionCommonData.getMinionId());
		po.setName(minionCommonData.getName());
		po.setGrade(minionCommonData.getMinionGrade());
		po.setLevel(minionCommonData.getMinionLevel());
		po.setGrowthpoints(minionCommonData.getMinionGrowthPoint());
		return po;
	}

	/**
	 * 将持久化实体还原为随从模型（字段与读取顺序与原 ResultSet 映射一致）。
	 * Restores the minion model from the persistence entity (same field order as the original ResultSet mapping).
	 * @param po 随从持久化实体 / minion persistence entity
	 * @param player 玩家 / player
	 * @return 随从公共数据 / minion common data
	 */
	private static MinionCommonData toMinionCommonData(PlayerMinionPo po, Player player) {
		MinionCommonData minionCommonData = new MinionCommonData(
			po.getObjectId(),
			po.getMinionId(),
			player.getObjectId(),
			po.getName(),
			po.getGrade(),
			po.getLevel(),
			po.getGrowthpoints()
		);

		minionCommonData.setBirthday(po.getBirthday());
		minionCommonData.setLock(po.getLocked() != null && po.getLocked() == 1);

		int minionId = po.getMinionId();
		if (minionId > 980013) {
			MinionDopingBag dopingBag = minionCommonData.getDopingBag();
			if (dopingBag != null) {
				String bag = po.getBuffBag();
				if (bag != null && !bag.isEmpty()) {
					String[] ids = bag.split(",");
					for (int i = 0; i < ids.length; i++) {
						if (i < 6 && !ids[i].isEmpty()) {
							try {
								int itemId = Integer.parseInt(ids[i]);
								dopingBag.setItem(itemId, i);
							} catch (NumberFormatException e) {
								log.warn(I18n.get("log.e8efb9fcad1f", minionCommonData.getObjectId(), ids[i]));
							}
						}
					}
				}
			}
		}

		return minionCommonData;
	}
}
