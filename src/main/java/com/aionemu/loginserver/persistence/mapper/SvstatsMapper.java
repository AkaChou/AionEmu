package com.aionemu.loginserver.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.loginserver.persistence.entity.SvstatsPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 在线统计映射层，全表更新为方言 SQL（Flex 拒绝无 WHERE 更新）。
 */
public interface SvstatsMapper extends BaseMapper<SvstatsPo> {

	/** 全表标记离线状态。 */
	@Update("UPDATE ls_svstats SET status = #{status}, current = #{current}, last_update = NOW()")
	int updateAllOffline(@Param("status") int status, @Param("current") int current);
}
