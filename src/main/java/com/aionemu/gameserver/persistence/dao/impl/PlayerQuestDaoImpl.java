package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.persistence.dao.PlayerQuestDao;
import com.aionemu.gameserver.persistence.entity.PlayerQuestPo;
import com.aionemu.gameserver.persistence.mapper.PlayerQuestMapper;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家任务状态数据访问实现。
 * Data access implementation for player quest states.
 */
@Slf4j
@Service
@Lazy
public class PlayerQuestDaoImpl extends ServiceImpl<PlayerQuestMapper, PlayerQuestPo> implements PlayerQuestDao {

	/** 筛选需新增的任务状态 / Filter quests to insert */
	private static final Predicate<QuestState> questsToAddPredicate =
		input -> input != null && PersistentState.NEW == input.getPersistentState();

	/** 筛选需更新的任务状态 / Filter quests to update */
	private static final Predicate<QuestState> questsToUpdatePredicate =
		input -> input != null && PersistentState.UPDATE_REQUIRED == input.getPersistentState();

	/** 筛选需删除的任务状态 / Filter quests to delete */
	private static final Predicate<QuestState> questsToDeletePredicate =
		input -> input != null && PersistentState.DELETED == input.getPersistentState();

	private final TransactionTemplate transactionTemplate;

	public PlayerQuestDaoImpl(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public QuestStateList load(Player player) {
		QuestStateList questStateList = new QuestStateList();
		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerQuestPo::getPlayerId).eq(player.getObjectId()))
				.forEach(po -> {
					Integer reward = po.getReward() == null ? 0 : po.getReward();
					QuestStatus status = QuestStatus.valueOf(po.getStatus());
					QuestState questState = new QuestState(po.getQuestId(), status, po.getQuestVars(),
						po.getCompleteCount(), po.getNextRepeatTime(), reward, po.getCompleteTime());
					questState.setPersistentState(PersistentState.UPDATED);
					questStateList.addQuest(po.getQuestId(), questState);
				});
		} catch (DataAccessException e) {
			log.error(I18n.get("log.1ce22f7418a0", player.getObjectId(), e.getMessage()), e);
		}
		return questStateList;
	}

	@Override
	public void store(Player player) {
		Collection<QuestState> qsList = player.getQuestStateList().getAllQuestState();
		if (GenericValidator.isBlankOrNull(qsList)) {
			return;
		}
		boolean hasDirty = false;
		for (QuestState qs : qsList) {
			if (qs.getPersistentState() != PersistentState.UPDATED) {
				hasDirty = true;
				break;
			}
		}
		if (!hasDirty) {
			return;
		}
		try {
			transactionTemplate.executeWithoutResult(_ -> store(player.getObjectId(), qsList));
			for (QuestState qs : qsList) {
				qs.setPersistentState(PersistentState.UPDATED);
			}
		} catch (RuntimeException e) {
			log.error(I18n.get("log.0b87b3157dfe", player.getObjectId()), e);
		}
	}

	@Override
	public void store(int playerId, Collection<QuestState> states) {
		requireTransaction();
		if (GenericValidator.isBlankOrNull(states)) {
			return;
		}
		deleteQuest(playerId, states);
		addQuests(playerId, states);
		updateQuests(playerId, states);
	}

	/**
	 * 要求存在活动事务：Spring 事务连接默认关闭自动提交。
	 * 旧实现直接断言 {@code !connection.getAutoCommit()}，此处保留等价断言。
	 * Requires an active transaction: Spring transactional connections default to auto-commit disabled.
	 * The legacy implementation asserted {@code !connection.getAutoCommit()}; this keeps the equivalent guard.
	 */
	private static void requireTransaction() {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalStateException("caller-owned quest transaction must disable auto-commit");
		}
	}

	/**
	 * 批量插入新增任务状态。
	 * Batch-inserts newly created quest states.
	 */
	private void addQuests(int playerId, Collection<QuestState> states) {
		Collection<QuestState> statesToAdd = Collections2.filter(states, questsToAddPredicate);
		if (GenericValidator.isBlankOrNull(statesToAdd)) {
			return;
		}
		List<PlayerQuestPo> rows = new ArrayList<>(statesToAdd.size());
		for (QuestState qs : statesToAdd) {
			rows.add(toPo(playerId, qs));
		}
		mapper.insertBatch(rows);
		log.debug("Inserted {} quests for player {}", rows.size(), playerId);
	}

	/**
	 * 更新已修改的任务状态。
	 * Updates quest states that require update.
	 */
	private void updateQuests(int playerId, Collection<QuestState> states) {
		Collection<QuestState> statesToUpdate = Collections2.filter(states, questsToUpdatePredicate);
		if (GenericValidator.isBlankOrNull(statesToUpdate)) {
			return;
		}
		for (QuestState qs : statesToUpdate) {
			UpdateChain.of(PlayerQuestPo.class)
				.set(PlayerQuestPo::getStatus, qs.getStatus().toString())
				.set(PlayerQuestPo::getQuestVars, qs.getQuestVars().getQuestVars())
				.set(PlayerQuestPo::getCompleteCount, qs.getCompleteCount())
				.set(PlayerQuestPo::getNextRepeatTime, qs.getNextRepeatTime())
				.set(PlayerQuestPo::getReward, qs.getRewardOrNull())
				.set(PlayerQuestPo::getCompleteTime, qs.getCompleteTime())
				.where(PlayerQuestPo::getPlayerId).eq(playerId)
				.and(PlayerQuestPo::getQuestId).eq(qs.getQuestId())
				.update();
		}
		log.debug("Updated {} quests for player {}", statesToUpdate.size(), playerId);
	}

	/**
	 * 删除标记为 DELETED 的任务状态。
	 * Deletes quest states marked as DELETED.
	 */
	private void deleteQuest(int playerId, Collection<QuestState> states) {
		Collection<QuestState> statesToDelete = Collections2.filter(states, questsToDeletePredicate);
		if (GenericValidator.isBlankOrNull(statesToDelete)) {
			return;
		}
		for (QuestState qs : statesToDelete) {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerQuestPo::getPlayerId).eq(playerId)
				.and(PlayerQuestPo::getQuestId).eq(qs.getQuestId()));
		}
		log.debug("Deleted {} quests for player {}", statesToDelete.size(), playerId);
	}

	/**
	 * 由领域任务状态构造持久化实体。
	 * Builds the persistence entity from a domain quest state.
	 */
	private static PlayerQuestPo toPo(int playerId, QuestState qs) {
		PlayerQuestPo po = new PlayerQuestPo();
		po.setPlayerId(playerId);
		po.setQuestId(qs.getQuestId());
		po.setStatus(qs.getStatus().toString());
		po.setQuestVars(qs.getQuestVars().getQuestVars());
		po.setCompleteCount(qs.getCompleteCount());
		po.setNextRepeatTime(qs.getNextRepeatTime());
		po.setReward(qs.getRewardOrNull());
		po.setCompleteTime(qs.getCompleteTime());
		return po;
	}
}
