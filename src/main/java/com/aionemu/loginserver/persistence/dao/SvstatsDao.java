package com.aionemu.loginserver.persistence.dao;

import com.aionemu.loginserver.persistence.entity.SvstatsPo;
import com.mybatisflex.core.service.IService;

/**
 * 游戏服在线统计（ls_svstats）数据访问层。
 */
public interface SvstatsDao extends IService<SvstatsPo> {

	void update_SvStats_Online(int server, int status, int current, int max);

	void update_SvStats_Offline(int server, int status, int current);

	/** 将全部服务器标记为离线（方言 SQL：无 WHERE 全表更新）。 */
	void update_SvStats_All_Offline(int status, int current);
}
