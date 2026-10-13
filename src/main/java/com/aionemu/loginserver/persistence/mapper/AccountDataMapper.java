package com.aionemu.loginserver.persistence.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import com.aionemu.loginserver.persistence.entity.AccountDataPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 账号数据映射层。
 * Mapper for account data.
 * <p>
 * {@code deleteInactiveAccounts} 的删除条件是 ls_account_time 上的行级子查询，无法用 Flex CRUD 表达，保留注解 SQL。
 * The delete condition of {@code deleteInactiveAccounts} is a row-level subquery over ls_account_time, not expressible
 * with Flex CRUD, so the annotated SQL is kept.
 * </p>
 */
public interface AccountDataMapper extends BaseMapper<AccountDataPo> {

	/** 删除不活跃账号（account_time.last_active 超期即删，行级子查询）。 */
	@Delete("DELETE FROM ls_account_data WHERE id IN "
		+ "(SELECT account_id FROM ls_account_time WHERE DATEDIFF(CURDATE(), last_active) > #{days})")
	int deleteInactiveAccounts(@Param("days") int days);
}
