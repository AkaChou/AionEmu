package com.aionemu.loginserver.persistence.dao;

import java.sql.Timestamp;
import java.util.Set;

import com.aionemu.loginserver.model.BannedIP;
import com.aionemu.loginserver.persistence.entity.BannedIpPo;
import com.mybatisflex.core.service.IService;

/**
 * IP 封禁（ls_banned_ip）数据访问层。
 */
public interface BannedIpDao extends IService<BannedIpPo> {

	/** 插入永久封禁（无过期时间）。 */
	BannedIP insert(String mask);

	/** 插入带过期时间的封禁；expireTime 为 null 表示永久。 */
	BannedIP insert(String mask, Timestamp expireTime);

	/** 插入封禁记录，成功时回写生成 ID。 */
	boolean insert(BannedIP bannedIP);

	boolean update(BannedIP bannedIP);

	boolean remove(String mask);

	boolean remove(BannedIP bannedIP);

	Set<BannedIP> getAllBans();

	/** 清理已过期的封禁（方言 SQL：time_end 条件删除）。 */
	void cleanExpiredBans();
}
