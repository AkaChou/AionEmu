package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.linked_skill.EquippedStigmasEntry;
import com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList;
import com.aionemu.gameserver.persistence.dao.PlayerStigmasEquippedDao;
import com.aionemu.gameserver.persistence.entity.PlayerStigmasEquippedPo;
import com.aionemu.gameserver.persistence.mapper.PlayerStigmasEquippedMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家已装备灵魂石（Stigma）列表数据访问实现。
 * Data access implementation for player equipped stigma items.
 */
@Slf4j
@Service
@Lazy
public class PlayerStigmasEquippedDaoImpl
	extends ServiceImpl<PlayerStigmasEquippedMapper, PlayerStigmasEquippedPo>
	implements PlayerStigmasEquippedDao {

	/**
	 * 需要插入的条目过滤条件 / Predicate for items to insert
	 */
	private static final Predicate<EquippedStigmasEntry> itemsToInsertPredicate = input -> input != null
		&& PersistentState.NEW == input.getPersistentState();

	/**
	 * 需要更新的条目过滤条件 / Predicate for items to update
	 */
	private static final Predicate<EquippedStigmasEntry> itemsToUpdatePredicate = input -> input != null
		&& PersistentState.UPDATE_REQUIRED == input.getPersistentState();

	/**
	 * 需要删除的条目过滤条件 / Predicate for items to delete
	 */
	private static final Predicate<EquippedStigmasEntry> itemsToDeletePredicate = input -> input != null
		&& PersistentState.DELETED == input.getPersistentState();

	@Override
	public PlayerEquippedStigmaList loadItemsList(int playerId) {
		List<EquippedStigmasEntry> items = new ArrayList<>();

		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerStigmasEquippedPo::getPlayerId).eq(playerId))
				.forEach(po -> items.add(new EquippedStigmasEntry(po.getItemId(), po.getItemName(),
					PersistentState.UPDATED)));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.484620fa0a55", playerId), e);
		}
		return new PlayerEquippedStigmaList(items);
	}

	@Override
	public void storeItems(Player player) {
		List<EquippedStigmasEntry> skills = Lists.newArrayList(player.getEquipedStigmaList().getAllItems());
		skills.addAll(Lists.newArrayList(player.getEquipedStigmaList().getDeletedItems()));
		boolean hasDirty = false;
		for (EquippedStigmasEntry skill : skills) {
			if (skill.getPersistentState() != PersistentState.UPDATED) {
				hasDirty = true;
				break;
			}
		}
		if (!hasDirty) {
			return;
		}
		try {
			DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> storeItemsInTransaction(player));
		} catch (RuntimeException e) {
			log.error(I18n.get("log.6790b402b724", player.getObjectId()), e);
			return;
		}
		markStored(player);
	}

	@Override
	public void storeItemsInTransaction(Player player) {
		List<EquippedStigmasEntry> skills = Lists.newArrayList(player.getEquipedStigmaList().getAllItems());
		skills.addAll(Lists.newArrayList(player.getEquipedStigmaList().getDeletedItems()));
		deleteItems(player, skills);
		addItems(player, skills);
		updateItems(player, skills);
	}

	@Override
	public void markStored(Player player) {
		player.getEquipedStigmaList().markStored();
	}

	/**
	 * 批量插入新增的灵魂石条目。
	 * Batch-inserts newly equipped stigma entries.
	 *
	 * @param player 玩家 / player
	 * @param items  条目列表 / entry list
	 */
	private void addItems(Player player, List<EquippedStigmasEntry> items) {
		Collection<EquippedStigmasEntry> skillsToInsert = Collections2.filter(items, itemsToInsertPredicate);
		if (GenericValidator.isBlankOrNull(skillsToInsert)) {
			return;
		}

		List<PlayerStigmasEquippedPo> rows = new ArrayList<>(skillsToInsert.size());
		for (EquippedStigmasEntry skill : skillsToInsert) {
			PlayerStigmasEquippedPo po = new PlayerStigmasEquippedPo();
			po.setPlayerId(player.getObjectId());
			po.setItemId(skill.getItemId());
			po.setItemName(skill.getItemName());
			rows.add(po);
		}
		mapper.insertBatch(rows);
	}

	/**
	 * 更新待更新的灵魂石条目。
	 * Updates stigma entries that require update.
	 *
	 * @param player 玩家 / player
	 * @param skills 条目列表 / entry list
	 */
	private void updateItems(Player player, List<EquippedStigmasEntry> skills) {
		Collection<EquippedStigmasEntry> skillsToUpdate = Collections2.filter(skills, itemsToUpdatePredicate);
		if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
			return;
		}

		for (EquippedStigmasEntry skill : skillsToUpdate) {
			UpdateChain.of(PlayerStigmasEquippedPo.class)
				.set(PlayerStigmasEquippedPo::getItemId, skill.getItemId())
				.set(PlayerStigmasEquippedPo::getItemName, skill.getItemName())
				.where(PlayerStigmasEquippedPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerStigmasEquippedPo::getItemId).eq(skill.getItemId())
				.update();
		}
	}

	/**
	 * 删除已标记删除的灵魂石条目。
	 * Deletes stigma entries marked as deleted.
	 *
	 * @param player 玩家 / player
	 * @param skills 条目列表 / entry list
	 */
	private void deleteItems(Player player, List<EquippedStigmasEntry> skills) {
		Collection<EquippedStigmasEntry> skillsToDelete = Collections2.filter(skills, itemsToDeletePredicate);
		if (GenericValidator.isBlankOrNull(skillsToDelete)) {
			return;
		}

		for (EquippedStigmasEntry skill : skillsToDelete) {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerStigmasEquippedPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerStigmasEquippedPo::getItemId).eq(skill.getItemId())
				.and(PlayerStigmasEquippedPo::getItemName).eq(skill.getItemName()));
		}
	}
}
