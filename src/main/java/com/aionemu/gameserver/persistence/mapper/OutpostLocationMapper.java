package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.OutpostLocationPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 outpost_location 的映射层。
 * Mapper for table outpost_location.
 *
 * <p>该表 DDL 无主键，无法使用 Flex 的按主键 CRUD；插入的 ON DUPLICATE KEY UPDATE 语义
 * 也无法用 Flex CRUD 表达，故两条语句均以注解逐字保留（仅 ? 改为 #{}）。
 * The table has no primary key in its DDL, so Flex CRUD by primary key is unavailable, and the
 * INSERT's ON DUPLICATE KEY UPDATE semantics cannot be expressed with Flex CRUD; both statements
 * are therefore kept verbatim as annotated SQL (only `?` became `#{}`).
 */
public interface OutpostLocationMapper extends BaseMapper<OutpostLocationPo> {

	/**
	 * 插入或更新前哨地点（原 INSERT_QUERY）。
	 * Inserts or updates an outpost location (formerly INSERT_QUERY).
	 *
	 * @param id   前哨 ID / outpost id
	 * @param race 阵营 / race
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `outpost_location` (`id`, `race`) VALUES (#{id}, #{race}) "
		+ "ON DUPLICATE KEY UPDATE `race` = VALUES(`race`)")
	int insertOutpostLocation(@Param("id") int id, @Param("race") String race);

	/**
	 * 更新前哨地点阵营（原 UPDATE_QUERY）。
	 * Updates an outpost location's race (formerly UPDATE_QUERY).
	 *
	 * @param race 阵营 / race
	 * @param id   前哨 ID / outpost id
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE `outpost_location` SET `race` = #{race} WHERE `id` = #{id}")
	int updateOutpostRace(@Param("race") String race, @Param("id") int id);
}
