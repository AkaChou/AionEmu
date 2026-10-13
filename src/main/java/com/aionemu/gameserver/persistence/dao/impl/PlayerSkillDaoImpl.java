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
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.persistence.dao.PlayerSkillDao;
import com.aionemu.gameserver.persistence.entity.PlayerSkillPo;
import com.aionemu.gameserver.persistence.mapper.PlayerSkillMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
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
	public void storeSkillInTransaction(int playerId, int skillId, int targetLevel) {
		mapper.grantSkill(playerId, skillId, targetLevel);
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
				deleteSkills(player, skills);
				addSkills(player, skills);
				updateSkills(player, skills);
				updateSkinSkills(player, skills);
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
	 * 插入 NEW 状态技能（逐行 upsert）。
	 * Inserts skills in NEW state (one upsert per row).
	 *
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 */
	private void addSkills(Player player, List<PlayerSkillEntry> skills) {
		Collection<PlayerSkillEntry> skillsToInsert = Collections2.filter(skills, skillsToInsertPredicate);

		if (GenericValidator.isBlankOrNull(skillsToInsert)) {
			return;
		}

		for (PlayerSkillEntry skill : skillsToInsert) {
			mapper.insertSkill(player.getObjectId(), skill.getSkillId(), skill.getSkillLevel());
		}
	}

	/**
	 * 更新 UPDATE_REQUIRED 状态技能等级。
	 * Updates skill levels in UPDATE_REQUIRED state.
	 *
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 */
	private void updateSkills(Player player, List<PlayerSkillEntry> skills) {
		Collection<PlayerSkillEntry> skillsToUpdate = Collections2.filter(skills, skillsToUpdatePredicate);

		if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
			return;
		}

		for (PlayerSkillEntry skill : skillsToUpdate) {
			UpdateChain.of(PlayerSkillPo.class)
				.set(PlayerSkillPo::getSkillLevel, skill.getSkillLevel())
				.where(PlayerSkillPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerSkillPo::getSkillId).eq(skill.getSkillId())
				.update();
		}
	}

	/**
	 * 更新 UPDATE_REQUIRED 状态技能皮肤信息。
	 * Updates skill skin info in UPDATE_REQUIRED state.
	 *
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 */
	private void updateSkinSkills(Player player, List<PlayerSkillEntry> skills) {
		Collection<PlayerSkillEntry> skillsToUpdate = Collections2.filter(skills, skillsToUpdatePredicate);

		if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
			return;
		}

		for (PlayerSkillEntry skill : skillsToUpdate) {
			UpdateChain.of(PlayerSkillPo.class)
				.set(PlayerSkillPo::getSkinId, skill.getSkinId())
				.set(PlayerSkillPo::getSkinActiveDate, skill.getSkinActiveTime())
				.set(PlayerSkillPo::getSkinExpireTime, skill.getSkinExpireTime())
				.set(PlayerSkillPo::getSkinActivated, skill.isActivated())
				.where(PlayerSkillPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerSkillPo::getSkillId).eq(skill.getSkillId())
				.update();
		}
	}

	/**
	 * 删除 DELETED 状态技能。
	 * Deletes skills in DELETED state.
	 *
	 * @param player 玩家 / player
	 * @param skills 技能条目列表 / list of skill entries
	 */
	private void deleteSkills(Player player, List<PlayerSkillEntry> skills) {
		Collection<PlayerSkillEntry> skillsToDelete = Collections2.filter(skills, skillsToDeletePredicate);

		if (GenericValidator.isBlankOrNull(skillsToDelete)) {
			return;
		}

		for (PlayerSkillEntry skill : skillsToDelete) {
			mapper.deleteByQuery(QueryWrapper.create()
				.where(PlayerSkillPo::getPlayerId).eq(player.getObjectId())
				.and(PlayerSkillPo::getSkillId).eq(skill.getSkillId()));
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
