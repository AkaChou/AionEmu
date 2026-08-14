package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;

/**
 * 旧角色名数据访问对象，负责检查与记录改名历史。
 * Old player-name data access object responsible for checking and recording rename history.
 *
 * @author synchro2
 */
public abstract class OldNamesDAO implements DAO {

	/**
	 * 判断名称是否曾被使用过（旧名）。
	 * Checks whether the name was previously used (old name).
	 *
	 * @param name 名称 / name
	 * @return 是否为旧名 / true if it is an old name
	 */
	public abstract boolean isOldName(String name);

	/**
	 * 插入一次改名记录。
	 * Inserts a rename history record.
	 *
	 * @param id 玩家 ID / player id
	 * @param oldname 旧名称 / old name
	 * @param newname 新名称 / new name
	 */
	public abstract void insertNames(int id, String oldname, String newname);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return OldNamesDAO.class.getName();
	}
}
