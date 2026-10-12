package com.aionemu.gameserver.persistence.dao;

import java.util.Set;

import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.persistence.entity.AnnouncementPo;
import com.mybatisflex.core.service.IService;

/**
 * 公告数据访问层。
 * Data access layer for server announcements.
 */
public interface AnnouncementDao extends IService<AnnouncementPo> {

	/**
	 * 获取全部公告。
	 * Gets all announcements.
	 * @return 公告集合 / announcement set
	 */
	Set<Announcement> getAnnouncements();

	/**
	 * 添加一条公告。
	 * Adds an announcement.
	 * @param announce 公告 / announcement
	 */
	void addAnnouncement(Announcement announce);

	/**
	 * 删除指定 ID 的公告。
	 * Deletes the announcement with the given ID.
	 * @param idAnnounce 公告 ID / announcement ID
	 * @return 是否成功 / whether successful
	 */
	boolean delAnnouncement(int idAnnounce);
}
