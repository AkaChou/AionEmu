package com.aionemu.loginserver.persistence.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.aionemu.loginserver.persistence.entity.AccountDataPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 账号数据映射层，含两条无法用 CRUD 表达的方言 SQL。
 */
public interface AccountDataMapper extends BaseMapper<AccountDataPo> {

	/** 会员到期恢复旧等级：仅当 expire 已过期时更新。 */
	@Update("UPDATE ls_account_data SET membership = old_membership, expire = NULL "
		+ "WHERE id = #{accountId} AND expire < CURRENT_TIMESTAMP")
	int restoreExpiredMembership(@Param("accountId") int accountId);

	/** 删除不活跃账号（account_time.last_active 超期即删，行级子查询）。 */
	@Delete("DELETE FROM ls_account_data WHERE id IN "
		+ "(SELECT account_id FROM ls_account_time WHERE DATEDIFF(CURDATE(), last_active) > #{days})")
	int deleteInactiveAccounts(@Param("days") int days);
}
