package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.persistence.dao.OldNameDao;
import com.aionemu.gameserver.persistence.entity.OldNamePo;
import com.aionemu.gameserver.persistence.mapper.OldNameMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 旧角色名数据访问实现。
 * Old player-name data access implementation.
 * @author synchro2
 */
@Slf4j
@Service
@Lazy
public class OldNameDaoImpl extends ServiceImpl<OldNameMapper, OldNamePo> implements OldNameDao {

	/**
	 * 判断名称是否曾被使用过。
	 * Checks whether the name has been used before.
	 * @param name 角色名 / character name
	 * @return 是否为旧名 / whether it is an old name
	 */
	@Override
	public boolean isOldName(final String name) {
		try {
			return mapper.selectCountByQuery(QueryWrapper.create().eq(OldNamePo::getOldName, name)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b006f4717fd6", name, ", is used, returning positive result"), e);
			return true;
		}
	}

	/**
	 * 记录角色改名历史。
	 * Records a character rename history entry.
	 * @param id 玩家 ID / player id
	 * @param oldname 旧名 / old name
	 * @param newname 新名 / new name
	 */
	@Override
	public void insertNames(final int id, final String oldname, final String newname) {
		try {
			OldNamePo po = new OldNamePo();
			po.setPlayerId(id);
			po.setOldName(oldname);
			po.setNewName(newname);
			mapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0f01a0baefea"), e);
		}
	}
}
