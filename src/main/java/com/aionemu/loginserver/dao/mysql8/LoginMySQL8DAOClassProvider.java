package com.aionemu.loginserver.dao.mysql8;

import com.aionemu.commons.database.dao.DAOClassProvider;

/**
 * 登录服 MySQL 8 DAO 类提供器。
 * Login-server MySQL 8 DAO class provider.
 */
public class LoginMySQL8DAOClassProvider implements DAOClassProvider {

	/**
	 * 返回 DAO 上下文名称。
	 * Returns the DAO context name.
	 *
	 * context name
	 */
	@Override
	public String contextName() {
		return "login";
	}

	/**
	 * 返回本提供器注册的全部 MySQL 8 DAO 类。
	 * Returns all MySQL 8 DAO classes registered by this provider.
	 *
	 * array of DAO classes
	 */
	@Override
	public Class<?>[] daoClasses() {
		return new Class<?>[] {
			MySQL8AccountDAO.class,
			MySQL8AccountPlayTimeDAO.class,
			MySQL8AccountTimeDAO.class,
			MySQL8BannedIpDAO.class,
			MySQL8BannedMacDAO.class,
			MySQL8GameServersDAO.class,
			MySQL8PlayerTransferDAO.class,
			MySQL8PremiumDAO.class,
			MySQL8SvStatsDAO.class,
			MySQL8TaskFromDBDAO.class,
		};
	}
}
