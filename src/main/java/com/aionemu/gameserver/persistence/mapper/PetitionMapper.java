package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Select;

import com.aionemu.gameserver.persistence.entity.PetitionPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 petitions 的映射层。
 * Mapper for table petitions.
 */
public interface PetitionMapper extends BaseMapper<PetitionPo> {

	/**
	 * 计算下一个可用的请愿 ID（方言聚合，保留 {@code COALESCE(MAX(id), 0) + 1}）。
	 * Computes the next available petition id (dialect aggregate, keeping {@code COALESCE(MAX(id), 0) + 1}).
	 * @return 下一个可用 ID / next available id
	 */
	@Select("SELECT COALESCE(MAX(id), 0) + 1 as nextid FROM petitions")
	int nextId();
}
