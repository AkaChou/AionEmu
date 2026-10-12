package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.persistence.entity.OldNamePo;
import com.mybatisflex.core.service.IService;

/**
 * 旧角色名数据访问层，负责检查与记录改名历史。
 * Old player-name data access layer responsible for checking and recording rename history.
 * @author synchro2
 */
public interface OldNameDao extends IService<OldNamePo> {

	/**
	 * 判断名称是否曾被使用过（旧名）。
	 * Checks whether the name was previously used (old name).
	 * @param name 名称 / name
	 * @return 是否为旧名 / true if it is an old name
	 */
	boolean isOldName(String name);

	/**
	 * 插入一次改名记录。
	 * Inserts a rename history record.
	 * @param id 玩家 ID / player id
	 * @param oldname 旧名称 / old name
	 * @param newname 新名称 / new name
	 */
	void insertNames(int id, String oldname, String newname);
}
