package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;

import com.mybatisflex.core.BaseMapper;
import com.aionemu.gameserver.persistence.entity.PlayerMinionPo;

/**
 * 表 player_minions 的映射层。
 * Mapper for table player_minions.
 */
public interface PlayerMinionMapper extends BaseMapper<PlayerMinionPo> {

	/**
	 * 插入随从记录；生日由库内 NOW() 求值，锁定与增益包取固定初值。
	 * Inserts a minion row; birthday is evaluated in-database via NOW(), lock and buff bag get fixed initial values.
	 * @param po 随从持久化实体 / minion persistence entity
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO player_minions (player_id, object_id, minion_id, name, grade, level, growthpoints, birthday, is_locked, buff_bag) VALUES (#{playerId}, #{objectId}, #{minionId}, #{name}, #{grade}, #{level}, #{growthpoints}, NOW(), 0, '')")
	int insertMinion(PlayerMinionPo po);
}
