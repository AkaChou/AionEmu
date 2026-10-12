package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.persistence.entity.ServerVariablePo;
import com.mybatisflex.core.service.IService;

/**
 * 服务器变量数据访问层。
 * Data access layer for server-wide key-value variables persistence.
 */
public interface ServerVariableDao extends IService<ServerVariablePo> {

	/**
	 * 加载指定服务器变量。
	 * Loads a server variable stored in the database.
	 * @param var 变量名 / variable name
	 * @return 变量整数值 / variable integer value
	 */
	int load(String var);

	/**
	 * 保存服务器变量。
	 * Stores a server variable.
	 * @param var 变量名 / variable name
	 * @param value 变量值 / variable value
	 * @return 是否保存成功 / true if stored
	 */
	boolean store(String var, int value);
}
