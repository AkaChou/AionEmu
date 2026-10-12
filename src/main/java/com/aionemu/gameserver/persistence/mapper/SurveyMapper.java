package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.gameserver.persistence.entity.SurveyPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 surveys 的映射层。
 * Mapper for table surveys.
 */
public interface SurveyMapper extends BaseMapper<SurveyPo> {

	/**
	 * 标记问卷已使用（保留库内 {@code NOW()} 求值）。
	 * Marks a survey as used (keeping in-database {@code NOW()} evaluation).
	 * @param used 使用标记 / used flag
	 * @param uniqueId 问卷唯一 ID / survey unique id
	 * @return 影响行数 / affected rows
	 */
	@Update("UPDATE `surveys` SET `used`=#{used}, used_time=NOW() WHERE `unique_id`=#{uniqueId}")
	int markUsed(@Param("used") int used, @Param("uniqueId") int uniqueId);
}
