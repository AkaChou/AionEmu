package com.aionemu.loginserver.dao.mysql8;

import com.aionemu.commons.database.dao.DAOClassProvider;

public class LoginMySQL8DAOClassProvider implements DAOClassProvider {

	@Override
	public String contextName() {
		return "login";
	}

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
