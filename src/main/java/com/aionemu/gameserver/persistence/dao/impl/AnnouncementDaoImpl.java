package com.aionemu.gameserver.persistence.dao.impl;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.persistence.dao.AnnouncementDao;
import com.aionemu.gameserver.persistence.entity.AnnouncementPo;
import com.aionemu.gameserver.persistence.mapper.AnnouncementMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 公告数据访问实现。
 * Data access implementation for server announcements.
 */
@Slf4j
@Service
@Lazy
public class AnnouncementDaoImpl extends ServiceImpl<AnnouncementMapper, AnnouncementPo> implements AnnouncementDao {

	@Override
	public Set<Announcement> getAnnouncements() {
		Set<Announcement> result = new HashSet<>();
		try {
			mapper.selectListByQuery(QueryWrapper.create().orderBy(AnnouncementPo::getId, true))
				.forEach(po -> result.add(new Announcement(po.getId(), po.getAnnounce(), po.getFaction(), po.getType(),
					po.getDelay())));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.3e42f415d09f"), e);
		}
		return result;
	}

	@Override
	public void addAnnouncement(Announcement announce) {
		try {
			AnnouncementPo po = new AnnouncementPo();
			po.setAnnounce(announce.getAnnounce());
			po.setFaction(announce.getFaction());
			po.setType(announce.getType());
			po.setDelay(announce.getDelay());
			mapper.insert(po);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b3b62f944143"), e);
		}
	}

	@Override
	public boolean delAnnouncement(int idAnnounce) {
		try {
			return mapper.deleteByQuery(QueryWrapper.create().where(AnnouncementPo::getId).eq(idAnnounce)) > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.7f5bab6b7889", idAnnounce), e);
			return false;
		}
	}
}
