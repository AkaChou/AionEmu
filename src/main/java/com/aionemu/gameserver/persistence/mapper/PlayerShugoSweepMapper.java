package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Delete;

import com.aionemu.gameserver.persistence.entity.PlayerShugoSweepPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 player_shugo_sweep 的映射层。
 * Mapper for table player_shugo_sweep.
 */
public interface PlayerShugoSweepMapper extends BaseMapper<PlayerShugoSweepPo> {

	/**
	 * 清空全部术古扫荡记录（无 WHERE 的 DELETE 被 Flex CRUD 拒绝，须保留注解 SQL）。
	 * Deletes every Shugo Sweep record (Flex CRUD refuses WHERE-less DELETE, hence annotated SQL).
	 * @return 影响行数 / affected rows
	 */
	@Delete("DELETE FROM `player_shugo_sweep`")
	int deleteAll();
}
