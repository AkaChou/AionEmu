package com.aionemu.gameserver.persistence.dao.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;
import com.aionemu.gameserver.persistence.dao.HouseScriptDao;
import com.aionemu.gameserver.persistence.entity.HouseScriptPo;
import com.aionemu.gameserver.persistence.mapper.HouseScriptMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 房屋脚本数据访问的 MyBatis-Flex 实现。
 * MyBatis-Flex implementation of the house scripts data access layer.
 */
@Slf4j
@Service
@Lazy
public class HouseScriptDaoImpl extends ServiceImpl<HouseScriptMapper, HouseScriptPo> implements HouseScriptDao {

	@Override
	public void addScript(int houseId, int position, String scriptXML) {
		updateScript(houseId, position, scriptXML);
	}

	@Override
	public PlayerScripts getPlayerScripts(int houseId) {
		PlayerScripts scripts = new PlayerScripts(houseId);
		try {
			mapper.selectListByQuery(QueryWrapper.create()
				.where(HouseScriptPo::getHouseId).eq(houseId)
				.orderBy(HouseScriptPo::getIndex).asc())
				.forEach(po -> scripts.addScript(po.getIndex(), po.getScript()));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.0121b4201018", houseId), e);
		}
		return scripts;
	}

	@Override
	public void updateScript(int houseId, int position, String scriptXML) {
		try {
			String scriptParam = (scriptXML == null || scriptXML.isEmpty()) ? null : scriptXML;
			mapper.mergeScript(houseId, position, scriptParam);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.22094aa47afe", houseId, position), e);
		}
	}
}
