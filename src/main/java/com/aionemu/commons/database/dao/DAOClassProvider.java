package com.aionemu.commons.database.dao;

public interface DAOClassProvider {

	String contextName();

	Class<?>[] daoClasses();
}
