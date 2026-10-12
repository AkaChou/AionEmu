package com.aionemu.gameserver.persistence.mapper;

import com.aionemu.gameserver.persistence.entity.PlayerQuestPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_quests 的映射层。
 * Mapper for table player_quests.
 */
public interface PlayerQuestMapper extends BaseMapper<PlayerQuestPo> {

	/** 插入新玩家任务（调用方事务原生 JDBC 使用）。 / Insert a new player quest (used by caller-owned native JDBC). */
	String INSERT_JDBC = "INSERT INTO `player_quests` (`player_id`, `quest_id`, `status`, `quest_vars`, `complete_count`, `next_repeat_time`, `reward`, `complete_time`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	/** 更新玩家任务状态（调用方事务原生 JDBC 使用）。 / Update a player quest state (used by caller-owned native JDBC). */
	String UPDATE_JDBC = "UPDATE `player_quests` SET `status` = ?, `quest_vars` = ?, `complete_count` = ?, `next_repeat_time` = ?, `reward` = ?, `complete_time` = ? WHERE `player_id` = ? AND `quest_id` = ?";

	/** 删除玩家任务（调用方事务原生 JDBC 使用）。 / Delete a player quest (used by caller-owned native JDBC). */
	String DELETE_JDBC = "DELETE FROM `player_quests` WHERE `player_id` = ? AND `quest_id` = ?";
}
