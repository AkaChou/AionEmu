package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
			DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {
				try (Connection con = DatabaseAccess.getConnection()) {
					storeItemsInTransaction(con, player);
				} catch (SQLException e) {
					throw new DataAccessException("Failed to store equipped stigmas", e) {
					};
				}
			});
		} catch (RuntimeException e) {
			log.error(I18n.get("log.6790b402b724", player.getObjectId()), e);
			return;
		}
		markStored(player);
	}

	@Override
	public void storeItemsInTransaction(Connection connection, Player player) throws SQLException {
		List<EquippedStigmasEntry> skills = Lists.newArrayList(player.getEquipedStigmaList().getAllItems());
		skills.addAll(Lists.newArrayList(player.getEquipedStigmaList().getDeletedItems()));
		deleteItems(connection, player, skills);
		addItems(connection, player, skills);
		updateItems(connection, player, skills);
	}

	@Override
	public void markStored(Player player) {
		player.getEquipedStigmaList().markStored();
	}

	/**
	 * 批量插入新增的灵魂石条目。
	 * Batch-inserts newly equipped stigma entries.
	 *
	 * @param con    数据库连接 / database connection
	 * @param player 玩家 / player
	 * @param items  条目列表 / entry list
	 */
	private void addItems(Connection con, Player player, List<EquippedStigmasEntry> items) throws SQLException {
		Collection<EquippedStigmasEntry> skillsToInsert = Collections2.filter(items, itemsToInsertPredicate);
		if (GenericValidator.isBlankOrNull(skillsToInsert)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(PlayerStigmasEquippedMapper.INSERT_QUERY_JDBC)) {
			for (EquippedStigmasEntry skill : skillsToInsert) {
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, skill.getItemId());
				ps.setString(3, skill.getItemName());
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}

	/**
	 * 批量更新待更新的灵魂石条目。
	 * Batch-updates stigma entries that require update.
	 *
	 * @param con    数据库连接 / database connection
	 * @param player 玩家 / player
	 * @param skills 条目列表 / entry list
	 */
	private void updateItems(Connection con, Player player, List<EquippedStigmasEntry> skills) throws SQLException {
		Collection<EquippedStigmasEntry> skillsToUpdate = Collections2.filter(skills, itemsToUpdatePredicate);
		if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(PlayerStigmasEquippedMapper.UPDATE_QUERY_JDBC)) {
			for (EquippedStigmasEntry skill : skillsToUpdate) {
				ps.setInt(1, skill.getItemId());
				ps.setString(2, skill.getItemName());
				ps.setInt(3, player.getObjectId());
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}

	/**
	 * 批量删除已标记删除的灵魂石条目。
	 * Batch-deletes stigma entries marked as deleted.
	 *
	 * @param con    数据库连接 / database connection
	 * @param player 玩家 / player
	 * @param skills 条目列表 / entry list
	 */
	private void deleteItems(Connection con, Player player, List<EquippedStigmasEntry> skills) throws SQLException {
		Collection<EquippedStigmasEntry> skillsToDelete = Collections2.filter(skills, itemsToDeletePredicate);
		if (GenericValidator.isBlankOrNull(skillsToDelete)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(PlayerStigmasEquippedMapper.DELETE_QUERY_JDBC)) {
			for (EquippedStigmasEntry skill : skillsToDelete) {
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, skill.getItemId());
				ps.setString(3, skill.getItemName());
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}
}
