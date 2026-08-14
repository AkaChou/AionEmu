package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;

/**
 * 服务器变量数据访问抽象层。
 * DAO for server-wide key-value variables persistence.
 *
 * @author Ben
 */
public abstract class ServerVariablesDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * @return 完整类名 / fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return ServerVariablesDAO.class.getName();
	}

	/**
	 * 加载指定服务器变量。
	 * Loads a server variable stored in the database.
	 *
	 * @param var 变量名 / variable name
	 * @return 变量整数值 / variable integer value
	 */
	public abstract int load(String var);

	/**
	 * 保存服务器变量。
	 * Stores a server variable.
	 *
	 * @param var 变量名 / variable name
	 * @param value 变量值 / variable value
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean store(String var, int value);

}
