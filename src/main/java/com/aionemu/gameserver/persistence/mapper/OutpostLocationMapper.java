package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.OutpostLocationPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 outpost_location 的映射层。
 * Mapper for table outpost_location.
 *
 * <p>该表 DDL 无主键，无法使用 Flex 的按主键 CRUD；插入的 {@code ON DUPLICATE KEY UPDATE} 语义
 * 也无法用 Flex 1.11.8 表达（该版本无 SQL 级 upsert），故插入语句逐字保留（仅 ? 改为 #{}）；
 * 更新走 DAO 的 Flex 更新链。
 * The table has no primary key in its DDL, so Flex CRUD by primary key is unavailable, and the
 * INSERT's {@code ON DUPLICATE KEY UPDATE} cannot be expressed with Flex 1.11.8 (no SQL-level upsert),
 * so the insert is kept verbatim (only `?` became `#{}`); the update uses the Flex update chain in the DAO.
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
}
