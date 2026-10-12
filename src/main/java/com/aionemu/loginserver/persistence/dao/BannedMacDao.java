package com.aionemu.loginserver.persistence.dao;

import java.util.Map;

import com.aionemu.loginserver.model.base.BannedMacEntry;
import com.aionemu.loginserver.persistence.entity.BannedMacPo;
import com.mybatisflex.core.service.IService;

/**
 * MAC 封禁（ls_banned_mac）数据访问层。
 */
public interface BannedMacDao extends IService<BannedMacPo> {

	Map<String, BannedMacEntry> load();

	/** 写入或替换一条 MAC 封禁（方言 SQL：REPLACE INTO）。 */
	boolean update(BannedMacEntry entry);

	boolean remove(String address);

	/** 清理已过期的封禁（方言 SQL：time < CURDATE()）。 */
	void cleanExpiredBans();
}
