package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.SiegeLocationPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 siege_locations 的映射层。
 * Mapper for table siege_locations.
 *
 * <p>ON DUPLICATE KEY UPDATE 语义无法用 Flex CRUD 表达，故以注解 SQL 逐字保留（仅 ? 改为 #{}）。
 * The ON DUPLICATE KEY UPDATE semantics cannot be expressed with Flex CRUD, so the statement is
 * kept verbatim as annotated SQL (only `?` became `#{}`).
 */
public interface SiegeLocationMapper extends BaseMapper<SiegeLocationPo> {

	/**
	 * 插入或更新攻城据点（原 INSERT_QUERY）。
	 * Inserts or updates a siege location (formerly INSERT_QUERY).
	 *
	 * @param id       据点 ID / location id
	 * @param race     阵营 / race
	 * @param legionId 军团 ID / legion id
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `siege_locations` (`id`, `race`, `legion_id`) VALUES (#{id}, #{race}, #{legionId}) "
		+ "ON DUPLICATE KEY UPDATE `race` = VALUES(`race`), `legion_id` = VALUES(`legion_id`)")
	int insertSiegeLocation(@Param("id") int id, @Param("race") String race, @Param("legionId") int legionId);
}
