package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.aionemu.gameserver.persistence.entity.BaseLocationPo;

/**
 * 表 base_location 的映射层。
 * Mapper for table base_location.
 */
public interface BaseLocationMapper extends BaseMapper<BaseLocationPo> {

	/**
	 * 按主键插入据点位置，已存在则覆盖阵营（MySQL upsert）。
	 * Inserts a base location by primary key, overwriting the race if it already exists (MySQL upsert).
	 * @param id 据点 ID / base id
	 * @param race 阵营名 / race name
	 * @return 影响行数 / affected rows
	 */
	@Insert("INSERT INTO `base_location` (`id`, `race`) VALUES (#{id}, #{race}) ON DUPLICATE KEY UPDATE `race` = VALUES(`race`)")
	int upsert(@Param("id") int id, @Param("race") String race);
}
