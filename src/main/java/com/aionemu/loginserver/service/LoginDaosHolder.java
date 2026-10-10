package com.aionemu.loginserver.service;

import java.util.List;
import java.util.function.Supplier;

import com.aionemu.commons.database.dao.DAOManager;
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
 * 登录服持久层依赖聚合的静态宿主：由 Spring 配置类在启动期绑定，供登录服各控制器、
 * 网络包与任务处理器经 {@link #daos()} 取用；未绑定（离线/单测环境）时按 safeDao 逐项兜底构建并缓存。
 * Static holder for the login-server persistence aggregate: bound by the Spring configuration at
 * startup and consumed by login controllers, network packets and task handlers via {@link #daos()};
 * falls back to per-DAO safeDao construction (cached) when unbound (offline/unit tests).
 */
public final class LoginDaosHolder {

	/**
	 * Spring 绑定的聚合实例。
	 * Spring-bound aggregate.
	 */
	private static volatile LoginDaos boundDaos;

	/**
	 * 由 Spring 配置类在启动期绑定聚合。
	 * Binds the aggregate from the Spring configuration at startup.
	 * @param daos 依赖聚合 / dependency aggregate
	 */
	public static void bindDaos(LoginDaos daos) {
		boundDaos = java.util.Objects.requireNonNull(daos, "daos must not be null");
	}

	/**
	 * 返回持久层依赖聚合；未绑定（离线/单测环境）时按 safeDao 逐项兜底构建并缓存。
	 * Returns the persistence aggregate; falls back to per-DAO safeDao construction when unbound (offline/unit tests).
	 * @return 依赖聚合 / dependency aggregate
	 */
	public static LoginDaos daos() {
		LoginDaos current = boundDaos;
		if (current == null) {
			synchronized (LoginDaosHolder.class) {
				current = boundDaos;
				if (current == null) {
					boundDaos = current = buildOfflineDaos();
				}
			}
		}
		return current;
	}

	/**
	 * 构建离线兜底聚合：逐项 safeDao 解析（容器未就绪时退回实现类无参构造）。
	 * Builds the offline fallback aggregate: each DAO resolved via safeDao (falls back to no-arg impl constructors).
	 */
	private static LoginDaos buildOfflineDaos() {
		return new LoginDaos(
				safeDao(AccountDAO.class, com.aionemu.loginserver.dao.impl.AccountDAO::new),
				safeDao(AccountTimeDAO.class, com.aionemu.loginserver.dao.impl.AccountTimeDAO::new),
				safeDao(AccountPlayTimeDAO.class, com.aionemu.loginserver.dao.impl.AccountPlayTimeDAO::new),
				safeDao(PremiumDAO.class, com.aionemu.loginserver.dao.impl.PremiumDAO::new),
				safeDao(VipDAO.class, com.aionemu.loginserver.dao.impl.VipDAO::new),
				safeDao(BannedIpDAO.class, com.aionemu.loginserver.dao.impl.BannedIpDAO::new),
				safeDao(BannedMacDAO.class, com.aionemu.loginserver.dao.impl.BannedMacDAO::new),
				safeDao(GameServersDAO.class, com.aionemu.loginserver.dao.impl.GameServersDAO::new),
				safeDao(PlayerTransferDAO.class, com.aionemu.loginserver.dao.impl.PlayerTransferDAO::new),
				safeDao(SvStatsDAO.class, com.aionemu.loginserver.dao.impl.SvStatsDAO::new),
				safeDao(TaskFromDBDAO.class, com.aionemu.loginserver.dao.impl.TaskFromDBDAO::new));
	}

	/**
	 * DAO 解析兜底：容器未就绪（离线/单测）时退回实现类无参构造。
	 * DAO resolution fallback: falls back to the no-arg impl constructor when the container is not ready (offline/unit tests).
	 */
	private static <T extends com.aionemu.commons.database.dao.DAO> T safeDao(Class<T> daoClass,
			Supplier<T> fallback) {
		if (DAOManager.isInitialized()) {
			try {
				return DAOManager.getDAO(daoClass);
			} catch (Exception e) {
				// 离线兜底路径，退回实现类无参构造。 / offline fallback, use the no-arg impl constructor
			}
		}
		return fallback.get();
	}

	/**
	 * 仅静态宿主，禁止实例化。
	 * Static holder only; not instantiable.
	 */
	private LoginDaosHolder() {
	}
}
