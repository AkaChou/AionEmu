package com.aionemu.gameserver.persistence.dao;

import com.mybatisflex.core.service.IService;
import com.aionemu.gameserver.persistence.entity.SkillMotionPo;
import com.aionemu.gameserver.persistence.mapper.SkillMotionMapper;

/**
 * 表 skill_motions 的数据访问层。
 * Data access layer for table skill_motions.
 */
public interface SkillMotionDao extends IService<SkillMotionPo> {

	/**
	 * 取得本表映射层：协变返回类型，用于调用 {@link SkillMotionMapper#upsert(SkillMotionPo)}。
	 * Returns this table's mapper: a covariant return type so {@link SkillMotionMapper#upsert(SkillMotionPo)}
	 * is reachable.
	 *
	 * @return 本表映射层 / this table's mapper
	 */
	@Override
	SkillMotionMapper getMapper();
}
