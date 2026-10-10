package com.aionemu.loginserver.service;

import java.util.Objects;

import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.dao.AccountPlayTimeDAO;
import com.aionemu.loginserver.dao.AccountTimeDAO;
import com.aionemu.loginserver.dao.BannedIpDAO;
import com.aionemu.loginserver.dao.BannedMacDAO;
import com.aionemu.loginserver.dao.GameServersDAO;
import com.aionemu.loginserver.dao.PlayerTransferDAO;
import com.aionemu.loginserver.dao.PremiumDAO;
import com.aionemu.loginserver.dao.SvStatsDAO;
import com.aionemu.loginserver.dao.TaskFromDBDAO;
import com.aionemu.loginserver.dao.VipDAO;

/**
 * 登录服持久层依赖聚合：不可变 record，由 Spring 配置类声明式构造器注入装配，
 * 并对全部 11 个 DAO 依赖执行及早失败判空，彻底取代 {@code DAOManager.getDAO} 静态查表。
 * Aggregated login-server persistence dependencies: an immutable record wired declaratively via
 * Spring constructor injection, with fail-fast validation of all 11 DAO dependencies, fully
 * replacing {@code DAOManager.getDAO} static table lookups.
 */
public record LoginDaos(
		AccountDAO accountDAO,
		AccountTimeDAO accountTimeDAO,
		AccountPlayTimeDAO accountPlayTimeDAO,
		PremiumDAO premiumDAO,
		VipDAO vipDAO,
		BannedIpDAO bannedIpDAO,
		BannedMacDAO bannedMacDAO,
		GameServersDAO gameServersDAO,
		PlayerTransferDAO playerTransferDAO,
		SvStatsDAO svStatsDAO,
		TaskFromDBDAO taskFromDBDAO) {

	/**
	 * 紧凑构造器：对全部持久层依赖执行及早失败判空。
	 * Compact constructor: fail-fast null validation for every persistence dependency.
	 */
	public LoginDaos {
		Objects.requireNonNull(accountDAO, "accountDAO must not be null");
		Objects.requireNonNull(accountTimeDAO, "accountTimeDAO must not be null");
		Objects.requireNonNull(accountPlayTimeDAO, "accountPlayTimeDAO must not be null");
		Objects.requireNonNull(premiumDAO, "premiumDAO must not be null");
		Objects.requireNonNull(vipDAO, "vipDAO must not be null");
		Objects.requireNonNull(bannedIpDAO, "bannedIpDAO must not be null");
		Objects.requireNonNull(bannedMacDAO, "bannedMacDAO must not be null");
		Objects.requireNonNull(gameServersDAO, "gameServersDAO must not be null");
		Objects.requireNonNull(playerTransferDAO, "playerTransferDAO must not be null");
		Objects.requireNonNull(svStatsDAO, "svStatsDAO must not be null");
		Objects.requireNonNull(taskFromDBDAO, "taskFromDBDAO must not be null");
	}
}
