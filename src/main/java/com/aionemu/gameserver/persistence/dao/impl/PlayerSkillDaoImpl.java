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
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.persistence.dao.PlayerSkillDao;
import com.aionemu.gameserver.persistence.entity.PlayerSkillPo;
import com.aionemu.gameserver.persistence.mapper.PlayerSkillMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家技能列表数据访问实现。
 * Data access implementation for player skill list persistence.
 *
 * @author SoulKeeper
 * @author IceReaper, orfeo087, Avol, AEJTester
 */
@Slf4j
@Service
@Lazy
public class PlayerSkillDaoImpl extends ServiceImpl<PlayerSkillMapper, PlayerSkillPo> implements PlayerSkillDao {

	/**
	 * 待插入技能谓词（NEW 状态） / Predicate for skills to insert (NEW state)
	 */
	private static final Predicate<PlayerSkillEntry> skillsToInsertPredicate = input -> input != null
		&& PersistentState.NEW == input.getPersistentState();

	/**
	 * 待更新技能谓词（UPDATE_REQUIRED 状态） / Predicate for skills to update (UPDATE_REQUIRED state)
	 */
	private static final Predicate<PlayerSkillEntry> skillsToUpdatePredicate = input -> input != null
		&& PersistentState.UPDATE_REQUIRED == input.getPersistentState();

	/**
	 * 待删除技能谓词（DELETED 状态） / Predicate for skills to delete (DELETED state)
	 */
	private static final Predicate<PlayerSkillEntry> skillsToDeletePredicate = input -> input != null
		&& PersistentState.DELETED == input.getPersistentState();

	@Override
	public PlayerSkillList loadSkillList(int playerId) {
		List<PlayerSkillEntry> skills = new ArrayList<>();

		try {
			mapper.selectListByQuery(QueryWrapper.create().where(PlayerSkillPo::getPlayerId).eq(playerId))
				.forEach(po -> skills.add(new PlayerSkillEntry(po.getSkillId(), false, false, po.getSkillLevel(),
					intValue(po.getSkinId()), po.getSkinActiveDate(), intValue(po.getSkinExpireTime()),
					Boolean.TRUE.equals(po.getSkinActivated()), PersistentState.UPDATED)));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d0d2b39236dc", playerId), e);
		}

		return new PlayerSkillList(skills);
	}

	@Override
	public boolean storeSkills(Player player) {
		List<PlayerSkillEntry> skillsActive = Lists.newArrayList(player.getSkillList().getAllSkills());
		skillsActive.addAll(Lists.newArrayList(player.getSkillList().getDeletedSkills()));
		boolean hasDirty = false;
		for (PlayerSkillEntry skill : skillsActive) {
			if (skill.getPersistentState() != PersistentState.UPDATED) {
				hasDirty = true;
				break;
			}
		}
		if (!hasDirty) {
			return true;
		}
		return store(player, skillsActive);
	}

	@Override
	public void storeSkillInTransaction(Connection connection, int playerId, int skillId, int targetLevel)
		throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(PlayerSkillMapper.GRANT_QUERY_JDBC)) {
			ps.setInt(1, playerId);
			ps.setInt(2, skillId);
			ps.setInt(3, targetLevel);
			ps.executeUpdate();
		}
	}

	/**
	 * 在事务中删除、插入、更新技能及皮肤。
	 * Deletes, inserts, and updates skills and skins within a transaction.
	 *
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 * @return 是否保存成功 / true if stored
	 */
	private boolean store(Player player, List<PlayerSkillEntry> skills) {
		try {
			DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {
				try (Connection con = DatabaseAccess.getConnection()) {
					deleteSkills(con, player, skills);
					addSkills(con, player, skills);
					updateSkills(con, player, skills);
					updateSkinSkills(con, player, skills);
				} catch (SQLException e) {
					throw new DataAccessException("Failed to store skill list", e) {
					};
				}
			});
		} catch (RuntimeException e) {
			log.error(I18n.get("log.6790b402b724", player.getObjectId()), e);
			return false;
		}

		for (PlayerSkillEntry skill : skills) {
			skill.setPersistentState(PersistentState.UPDATED);
		}
		return true;
	}

	/**
	 * 批量插入 NEW 状态技能。
	 * Batch-inserts skills in NEW state.
	 *
	 * @param con    连接 / connection
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 */
	private void addSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {
		Collection<PlayerSkillEntry> skillsToInsert = Collections2.filter(skills, skillsToInsertPredicate);

		if (GenericValidator.isBlankOrNull(skillsToInsert)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(PlayerSkillMapper.INSERT_QUERY_JDBC)) {
			for (PlayerSkillEntry skill : skillsToInsert) {
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, skill.getSkillId());
				ps.setInt(3, skill.getSkillLevel());
				ps.addBatch();
			}

			ps.executeBatch();
		}
	}

	/**
	 * 批量更新 UPDATE_REQUIRED 状态技能等级。
	 * Batch-updates skill levels in UPDATE_REQUIRED state.
	 *
	 * @param con    连接 / connection
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 */
	private void updateSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {
		Collection<PlayerSkillEntry> skillsToUpdate = Collections2.filter(skills, skillsToUpdatePredicate);

		if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(PlayerSkillMapper.UPDATE_QUERY_JDBC)) {
			for (PlayerSkillEntry skill : skillsToUpdate) {
				ps.setInt(1, skill.getSkillLevel());
				ps.setInt(2, player.getObjectId());
				ps.setInt(3, skill.getSkillId());
				ps.addBatch();
			}

			ps.executeBatch();
		}
	}

	/**
	 * 批量更新 UPDATE_REQUIRED 状态技能皮肤信息。
	 * Batch-updates skill skin info in UPDATE_REQUIRED state.
	 *
	 * @param con    连接 / connection
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 */
	private void updateSkinSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {
		Collection<PlayerSkillEntry> skillsToUpdate = Collections2.filter(skills, skillsToUpdatePredicate);

		if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(PlayerSkillMapper.UPDATE_SKIN_QUERY_JDBC)) {
			for (PlayerSkillEntry skill : skillsToUpdate) {
				ps.setInt(1, skill.getSkinId());
				ps.setTimestamp(2, skill.getSkinActiveTime());
				ps.setInt(3, skill.getSkinExpireTime());
				ps.setBoolean(4, skill.isActivated());
				ps.setInt(5, player.getObjectId());
				ps.setInt(6, skill.getSkillId());
				ps.addBatch();
			}

			ps.executeBatch();
		}
	}

	/**
	 * 批量删除 DELETED 状态技能。
	 * Batch-deletes skills in DELETED state.
	 *
	 * @param con    连接 / connection
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 */
	private void deleteSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {
		Collection<PlayerSkillEntry> skillsToDelete = Collections2.filter(skills, skillsToDeletePredicate);

		if (GenericValidator.isBlankOrNull(skillsToDelete)) {
			return;
		}

		try (PreparedStatement ps = con.prepareStatement(PlayerSkillMapper.DELETE_QUERY_JDBC)) {
			for (PlayerSkillEntry skill : skillsToDelete) {
				ps.setInt(1, player.getObjectId());
				ps.setInt(2, skill.getSkillId());
				ps.addBatch();
			}

			ps.executeBatch();
		}
	}

	/**
	 * 可空整数列的空值归一化，保持原 rs.getInt 读取语义（NULL 视为 0）。
	 * Normalizes nullable integer columns on read, keeping the legacy rs.getInt semantics (NULL becomes 0).
	 *
	 * @param value 可空值 / nullable value
	 * @return 归一化后的整数 / normalized int
	 */
	private static int intValue(Integer value) {
		return value == null ? 0 : value;
	}
}
